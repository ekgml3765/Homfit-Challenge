package com.ssafy.homfit.controller;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.homfit.api.ChallengeRepository;
import com.ssafy.homfit.api.TodayChallengeRepository;
import com.ssafy.homfit.model.Bookmark;
import com.ssafy.homfit.model.Challenge;
import com.ssafy.homfit.model.Favorite;
import com.ssafy.homfit.model.Feed;
import com.ssafy.homfit.model.Review;
import com.ssafy.homfit.model.Tag;
import com.ssafy.homfit.model.TodayChallenge;
import com.ssafy.homfit.model.User;
import com.ssafy.homfit.model.UserRate;
import com.ssafy.homfit.model.service.BookmarkService;
import com.ssafy.homfit.model.service.ChallengeService;
import com.ssafy.homfit.model.service.FavoriteService;
import com.ssafy.homfit.model.service.FeedService;
import com.ssafy.homfit.model.service.ReviewService;
import com.ssafy.homfit.model.service.S3Service;
import com.ssafy.homfit.model.service.TagService;

/**
 * @author 황다희
 *
 */
@RestController
@RequestMapping("/challenge")
public class ChallengeController {

	private static final Logger logger = LoggerFactory.getLogger(ChallengeController.class);
	private static final String SUCCESS = "success";
	private static final String FAIL = "fail";

	@Autowired
	ChallengeService challengeService;
	@Autowired
	BookmarkService bookmarkService;
	@Autowired
	TagService tagService;
	@Autowired
	ReviewService reviewService;
	@Autowired
	FavoriteService favoriteService;
	@Autowired
	FeedService feedService;

	@Autowired
	S3Service s3service;

	@Autowired
	RedisTemplate<String, Object> redisTemplate;
	@Autowired
	private ChallengeRepository challengeRepository;
	@Autowired
	private TodayChallengeRepository todayRepository;

	/**
	 * 테스트코드
	 * 
	 * @throws ParseException
	 */
	@GetMapping("/test")
	public ResponseEntity<String> testChallenge() throws ParseException {

		return new ResponseEntity<String>("hi", HttpStatus.NO_CONTENT);
	}

	/** 챌린지 관리 페이지 */
	@GetMapping("/management/{kind}")
	public ResponseEntity<List<Challenge>> challengeManagement(@RequestParam String uid, @PathVariable int kind) {

		List<Challenge> returnList = new ArrayList<Challenge>();
		List<Challenge> cacheList = (List<Challenge>) challengeRepository.findAll();
		List<TodayChallenge> todayList = (List<TodayChallenge>) todayRepository.findAll();
		// 카테고리 정렬은 front에서

		if (kind == 0) { // kind - 0 : 오늘 할 챌린지
			// 회원이 참가중이고 진행중인 챌린지 가져옴
			int[] c_id = challengeService.selectIngChallenge(uid);
			// 그 id값이 오늘 해야될 챌린지에 있으면 추가.
			for (int i = 0; i < c_id.length; i++) {
				if (todayRepository.findById(c_id[i]).isPresent()) {
					returnList.add(challengeRepository.findById(c_id[i]).get());
				}
			}
			// returnList에 담음
		} else if (kind == 1) {// kind - 1 : 진행전 챌린지
			int[] c_id = challengeService.selectPreChallenge(uid);
			for (int i = 0; i < c_id.length; i++) {
				Optional<Challenge> opt = challengeRepository.findById(c_id[i]);
				if (opt.isPresent()) {
					returnList.add(opt.get());
				}
			}
		} else if (kind == 2) { // kind - 2 : 진행중 챌린지
			int[] c_id = challengeService.selectIngChallenge(uid);
			for (int i = 0; i < c_id.length; i++) {
				Optional<Challenge> opt = challengeRepository.findById(c_id[i]);
				if (opt.isPresent()) {
					returnList.add(opt.get());
				}
			}
		} else if (kind == 3) {// kind - 3 : 완료한 챌린지
			int[] c_id = challengeService.selectEndChallenge(uid);
			for (int i = 0; i < c_id.length; i++) {
				Optional<Challenge> opt = challengeRepository.findById(c_id[i]);
				if (opt.isPresent()) {
					returnList.add(opt.get());
				}
			}
		} else { // kind - 4 : 개설한 챌린지
			for (Challenge challenge : cacheList) {
				if (challenge.getMake_uid().equals(uid)) {
					returnList.add(challenge);
				}
			}
		}

		return new ResponseEntity<List<Challenge>>(returnList, HttpStatus.OK);
	}

	/** 참여한 챌린지 관리 상세페이지 */
	@GetMapping("/detailManagement/{challengeId}")
	public ResponseEntity<HashMap<String, Object>> detailManagementChallenge(@RequestParam String uid,
			@PathVariable int challengeId) {

		HashMap<String, Object> map = new HashMap<String, Object>();

		// List<Challenge> cacheList = (List<Challenge>) challengeRepository.findAll();
		Optional<Challenge> opt = challengeRepository.findById(challengeId);
		if (!opt.isPresent()) { // 혹여나 없다면
			return new ResponseEntity<HashMap<String, Object>>(map, HttpStatus.NO_CONTENT);
		} else {
			Challenge challenge = opt.get();

			int average_rate = challenge.getAverage_rate();
			double total_cnt = challenge.getCertification(); // 소수점계산을 위해
			int user_cnt = feedService.selectUserFeed(uid, challengeId).length;
			double div = (user_cnt / total_cnt);
			int user_rate = (int) Math.round(div * 100); // 소수 첫번째자리 반올림

			// 현재 나의 달성률 (실시간)
			map.put("user_rate", user_rate);
			// 회원이 지금까지 인증한 피드 수
			map.put("user_cnt", user_cnt);
			// 챌린지 평균 달성률
			map.put("average_rate", average_rate);
			// 챌린지 총 인증해야할 갯수
			map.put("total_cnt", (int) total_cnt);

			// 오늘해야될 챌린지인지 체크
			Optional<TodayChallenge> today_opt = todayRepository.findById(challengeId);
			if (!today_opt.isPresent()) {
				map.put("today_cnt", 0);
				map.put("imgList", null);
			} else {// 오늘 해야된다면
				int today_cnt = challenge.getDay_certify_count();
				map.put("today_cnt", today_cnt);// 하루인증 횟수
				List<Feed> imgList = feedService.selectFeedImg(uid, challengeId);// 오늘 날짜에 인증한 사진이 있다면 리스트
				map.put("imgList", imgList);
			}

			// 후기리스트 주기
			Review r = reviewService.selectReview(uid, challengeId);
			map.put("review", r);

		}
		return new ResponseEntity<HashMap<String, Object>>(map, HttpStatus.OK);
	}

	/** 챌린지 후기 작성 */
	@PostMapping("/review")
	public ResponseEntity<String> writeReview(@RequestBody Review review) {

		if (reviewService.writeReview(review)) {
			return new ResponseEntity<String>(SUCCESS, HttpStatus.OK);
		}
		return new ResponseEntity<String>(FAIL, HttpStatus.NO_CONTENT);

	}

	/** 챌린지 검색 */
	@GetMapping("/search")
	public ResponseEntity<List<Challenge>> searchChallenge(@RequestParam String keyword, @RequestParam int kind) {

		List<Challenge> returnList = new ArrayList<Challenge>(); // 반환리스트
		List<Challenge> cacheList = (List<Challenge>) challengeRepository.findAll();

		// kind = 0 -> 제목기반 검색
		if (kind == 0) {
			for (Challenge challenge : cacheList) {
				if (challenge.getChallenge_title().contains(keyword)) {
					returnList.add(challenge);
				}
			}
		} else {// kind = 1 -> 태그기반 검색

			Tag tag = tagService.selectTag(keyword);
			if (tag != null) {
				int tagId = tag.getTag_id();
				Tag[] tagList = tagService.selectChallengeInTag(tagId);
				for (int i = 0; i < tagList.length; i++) {
					for (Challenge challenge : cacheList) {
						if (challenge.getChallenge_id() == tagList[i].getChallenge_id()) {
							returnList.add(challenge);
							break;
						}

					}
				}
			}
		}

		// 정렬 - 인기순
		Collections.sort(returnList, new Comparator<Challenge>() {
			@Override
			public int compare(Challenge o1, Challenge o2) {
				return o2.getPeople() - o1.getPeople();
			}
		});

		return new ResponseEntity<List<Challenge>>(returnList, HttpStatus.OK);

	}

	/** 추천 챌린지 */
	@GetMapping("/recommend/{kind}")
	public ResponseEntity<HashMap<String, Object>> recommendChallenge(@RequestParam String uid,
			@PathVariable int kind) {

		HashMap<String, Object> map = new HashMap<String, Object>();
		Favorite userFavorite = favoriteService.selectUserInfo(uid);
		String fit = userFavorite.getFit_list();
		String body = userFavorite.getBody_list();
		String day = userFavorite.getDay_list();

		// string -> string[]
		String[] fit_string = fit.substring(1, fit.length() - 1).split(",");
		String[] body_string = body.substring(1, body.length() - 1).split(",");
		String[] day_string = day.substring(1, day.length() - 1).split(",");

		// string[] -> int[]
		int[] fit_arr = Arrays.asList(fit_string).stream().mapToInt(Integer::parseInt).toArray();

		List<Challenge> cacheList = (List<Challenge>) challengeRepository.findAll();
		List<Challenge> returnList = new ArrayList<Challenge>();

		// 진행중, 완료중 챌린지는 제외
		for (Iterator<Challenge> it = cacheList.iterator(); it.hasNext();) {
			Challenge value = it.next();
			if (value.getCheck_date() == 1 || value.getCheck_date() == 2) {
				it.remove();
			}
		}

		if (kind == 1) { // 선호 카테고리 kind - 1
			for (int i = 0; i < fit_arr.length; i++) {
				for (Challenge challenge : cacheList) {
					if (challenge.getFit_id() == fit_arr[i])
						returnList.add(challenge);
				}
			}
		} else if (kind == 2) { // 선호 부위 kind - 2
			for (int i = 0; i < body_string.length; i++) {
				for (Challenge challenge : cacheList) {
					if (challenge.getBodylist_string().contains(body_string[i])) {
						returnList.add(challenge);
					}
				}
			}
		} else if (kind == 3) { // 선호 요일 kind - 3
			for (Challenge challenge : cacheList) {
				String s = challenge.getDaylist_string().replace(" ", "");
				if (day.equals(s))
					returnList.add(challenge);
			}

		} else if (kind == 4) { // 나이, 성별 추천 kind - 4 //보류

		}

		map.put("returnList", returnList);
		map.put("fit", fit_string);
		map.put("body", body_string);
		map.put("day", day_string);

		return new ResponseEntity<HashMap<String, Object>>(map, HttpStatus.OK);
	}

	/** 챌린지 참여 */
	@PostMapping("/join/{challengeId}")
	@Transactional
	public ResponseEntity<String> joinChallenge(@PathVariable int challengeId, @RequestBody User user) {
		String uid = user.getUid();
		if (challengeService.joinChallenge(challengeId, uid)) {
			// 챌린지 참여시 캐시 people ++;
			Optional<Challenge> opt = challengeRepository.findById(challengeId);
			Challenge ch = opt.get();
			int people = ch.getPeople();
			ch.setPeople(people + 1);
			challengeRepository.save(ch);
			return new ResponseEntity<String>(SUCCESS, HttpStatus.OK);
		}
		return new ResponseEntity<String>(FAIL, HttpStatus.NO_CONTENT);
	}

	/** 챌린지 참여 삭제 -> 참여자 일때만, 개설자는 챌린지 삭제로 가야함 */
	@DeleteMapping("/join/{challengeId}")
	@Transactional
	public ResponseEntity<String> quitChallenge(@PathVariable int challengeId, @RequestParam String uid) {

		if (challengeService.quitChallenge(challengeId, uid)) {
			// 챌린지 참여 삭제시 캐시 people --;
			Optional<Challenge> opt = challengeRepository.findById(challengeId);
			Challenge ch = opt.get();
			int people = ch.getPeople();
			ch.setPeople(people - 1);
			challengeRepository.save(ch);
			return new ResponseEntity<String>(SUCCESS, HttpStatus.OK);
		}
		return new ResponseEntity<String>(FAIL, HttpStatus.NO_CONTENT);
	}

	/** 챌린지 등록 */
	@PostMapping
	@Transactional
	public ResponseEntity<String> insertChallenge(@ModelAttribute Challenge challenge) {

		HttpStatus status = HttpStatus.OK;
		String result = FAIL;

		try {
			int kind = challenge.getKind(); // 챌린지 종류 -> 1-운동, 2-식단
			int dayList[] = challenge.getDayList();// 요일
			String tagList[] = challenge.getTagList();// 태그
			int bodyList[] = challenge.getBodyList();// 부위

			if (kind == 0 || dayList == null || dayList.length == 0) { // *종류와 요일은 필수값
				throw new Exception();
			} else {
				if (kind == 1 && (bodyList == null || bodyList.length == 0)) {// *운동이라면 부위선택 필수
					throw new Exception();
				}
				challenge.setDaylist_string(Arrays.toString(dayList));
				challenge.setBodylist_string(Arrays.toString(bodyList));

				// 0. 총 인증해야할 사진 수 계산
				int cert_day = certification_day(dayList, challenge.getStart_date(), challenge.getDay_certify_count(),
						challenge.getPeriod());
				challenge.setCertification(cert_day);

				// 1. 사진세팅- 사진 있을 경우만 url 저장
				if (challenge.getChallengeImgFile() != null) {
					challenge.setChallenge_img(s3service.uploadImg(challenge.getChallengeImgFile()));
				}
				if (challenge.getGoodImgFile() != null) {
					challenge.setGood_img(s3service.uploadImg(challenge.getGoodImgFile()));
				}
				if (challenge.getBadImgFile() != null) {
					challenge.setBad_img(s3service.uploadImg(challenge.getBadImgFile()));
				}

				challengeService.writeChallenge(challenge); // 입력
				int challengeId = challenge.getChallenge_id();

				// 2. 요일처리 - 동적쿼리
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("challenge_id", challengeId);
				map.put("list", IntStream.of(dayList).boxed().collect(Collectors.toList()));
				challengeService.writeChallengeDay(map);

				// 3. 부위처리 (운동일때만)
				if (kind == 1) {
					for (int i = 0; i < bodyList.length; i++) {
						HashMap<String, Integer> map_body = new HashMap<String, Integer>();
						map_body.put("challenge_id", challengeId);
						map_body.put("body_id", bodyList[i]);
						challengeService.writeChallengeBody(map_body);
					}
				}

				// 4. 태그처리 - 입력받았을 경우만
				if (tagList != null && tagList.length != 0) {
					for (int i = 0; i < tagList.length; i++) {
						HashMap<String, Integer> map_tag = new HashMap<String, Integer>();
						map_tag.put("challenge_id", challengeId);
						Tag tag = tagService.selectTag(tagList[i]);
						if (tag == null) { // 태그가 없다면 추가
							Tag addTag = new Tag(tagList[i]);
							tagService.writeTag(addTag);
							map_tag.put("tag_id", addTag.getTag_id());
							tagService.writeTagInChallenge(map_tag); // tag in challenge
							continue;
						}
						map_tag.put("tag_id", tag.getTag_id());
						tagService.writeTagInChallenge(map_tag); // tag in challenge
					}
				}

				// 5. 개설자는 참여테이블에 바로 insert
				challengeService.joinChallenge(challengeId, challenge.getMake_uid());
				result = Integer.toString(challengeId); // 개설 성공시 challengeID반환

				// 6. 캐시insert
				challenge.setPeople(1); // 사람수
				challenge.setNick_name(challengeService.selectUserNickname(challengeId)); // 닉네임
				challengeRepository.save(challenge);

			}

		} catch (Exception e) {
			logger.error("챌린지 등록 실패 : {}", e);
			result = e.getMessage();
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		return new ResponseEntity<String>(result, status);
	}

	/** 챌린지 수정 */
	@PutMapping("{challengeId}")
	@Transactional
	public ResponseEntity<String> updateChallenge(@PathVariable int challengeId, @ModelAttribute Challenge challenge) {

		HttpStatus status = HttpStatus.OK;
		String result = FAIL;

		String tagList[] = challenge.getTagList();// 태그
		int challenge_id = challengeId;

		try {
			// 1. 사진세팅 - 사진 있을 경우만 url 저장
			if (challenge.getChallengeImgFile() != null) {
				challenge.setChallenge_img(s3service.uploadImg(challenge.getChallengeImgFile()));
			}
			if (challenge.getGoodImgFile() != null) {
				challenge.setGood_img(s3service.uploadImg(challenge.getGoodImgFile()));
			}
			if (challenge.getBadImgFile() != null) {
				challenge.setBad_img(s3service.uploadImg(challenge.getBadImgFile()));
			}

			if (challengeService.updateChallenge(challenge)) {

				// 0. 기존태그 있는지 없는지 체크
				Tag[] check = tagService.selectTagInChallenge(challenge_id);

				if (check.length != 0) { // 삭제할 태그가 있다면
					// 1. 기존 태그 삭제
					if (tagService.deleteTagInChallenge(challenge_id)) {
					} else {
						throw new Exception();
					}
				}
				// 2. 수정한 태그 넣기
				for (int i = 0; i < tagList.length; i++) {
					HashMap<String, Integer> map_tag = new HashMap<String, Integer>();
					map_tag.put("challenge_id", challenge_id);
					Tag tag = tagService.selectTag(tagList[i]);
					if (tag == null) { // 태그가 없다면 추가
						Tag addTag = new Tag(tagList[i]);
						tagService.writeTag(addTag);
						map_tag.put("tag_id", addTag.getTag_id());
						tagService.writeTagInChallenge(map_tag); // tag in challenge
						continue;
					}
					map_tag.put("tag_id", tag.getTag_id());
					tagService.writeTagInChallenge(map_tag); // tag in challenge
				}

				// 3. 캐시 업데이트
				challengeRepository.save(challenge);
				result = Integer.toString(challenge_id); // 개설 성공시 challengeID반영

			} else {
				throw new Exception(); // 챌린지 없을경우
			}
		} catch (Exception e) {
			logger.error("챌린지 등록 실패 : {}", e);
			result = e.getMessage();
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}

		return new ResponseEntity<String>(result, status);

	}

	/** 챌린지 삭제 - 태그테이블은 연쇄삭제 x */
	@DeleteMapping("{challengeId}")
	@Transactional
	public ResponseEntity<String> deleteChallenge(@PathVariable int challengeId) {

		if (challengeService.deleteChallenge(challengeId)) {
			challengeRepository.deleteById(challengeId); // 캐시에서도 삭제
			return new ResponseEntity<String>(SUCCESS, HttpStatus.OK);
		}
		return new ResponseEntity<String>(FAIL, HttpStatus.NO_CONTENT);
	}

	/** 해당 챌린지 user의 참여여부, user의 북마크 */
	@GetMapping("/user/{challengeId}")
	public ResponseEntity<HashMap<String, String>> userInChallenge(@PathVariable int challengeId,
			@RequestParam String uid) {

		HashMap<String, String> map = new HashMap<String, String>();

		// 참여 여부 확인
		String participant = challengeService.selectParticipant(challengeId, uid);
		if (participant != null) {
			map.put("participant", "1"); // 참여 o -> true
		} else {
			map.put("participant", "0"); // false
		}
		// 북마크 했는지 확인
		String bookmark = bookmarkService.selectBookmark(challengeId, uid);
		if (bookmark != null) {
			map.put("bookmark", "1"); // 참여 o -> true
		} else {
			map.put("bookmark", "0"); // false
		}

		return new ResponseEntity<HashMap<String, String>>(map, HttpStatus.OK);
	}

	/** 챌린지 후기 리스트 반환 */
	@GetMapping("/review/{challengeId}/{sort}")
	public ResponseEntity<List<Review>> ReviewList(@PathVariable int challengeId, @PathVariable int sort) {
		List<Review> returnList = reviewService.selectAllReview(challengeId);
		if (returnList.size() == 0) {
			return new ResponseEntity<List<Review>>(returnList, HttpStatus.NO_CONTENT);
		}

		// 정렬 - sort = 0: 최신 -> 기본
		if (sort == 1) { // sort = 1:평점높
			Collections.sort(returnList, new Comparator<Review>() {
				@Override
				public int compare(Review o1, Review o2) {
					return o2.getStar_point() - o1.getStar_point();
				}
			});
		} else if (sort == 2) {// sort = 2:평점낮
			Collections.sort(returnList, new Comparator<Review>() {
				@Override
				public int compare(Review o1, Review o2) {
					return o1.getStar_point() - o2.getStar_point();
				}
			});
		}

		return new ResponseEntity<List<Review>>(returnList, HttpStatus.OK);
	}

	/** 챌린지 상세보기 -> 완료된 챌린지는 후기 최신순 3개담음 */
	@GetMapping("{challengeId}")
	public ResponseEntity<HashMap<String, Object>> detailChallenge(@PathVariable int challengeId) {

		HashMap<String, Object> map = new HashMap<String, Object>();
		List<Review> reviewList = new ArrayList<Review>();
		Challenge challenge = new Challenge();
		Optional<Challenge> opt = challengeRepository.findById(challengeId); // 캐시에서 가져옴
		if (!opt.isPresent()) {
			map.put("challenge", challenge);
			map.put("reivew", reviewList);
			return new ResponseEntity<HashMap<String, Object>>(map, HttpStatus.NO_CONTENT);
		} else {
			challenge = opt.get();
			// 태그리스트
			Tag tag[] = tagService.selectTagInChallenge(challengeId);
			if (tag.length != 0) {
				String[] taglist = new String[tag.length];
				for (int i = 0; i < tag.length; i++) {
					taglist[i] = tag[i].getTag_name();
				}
				challenge.setTagList(taglist);
			}
			// 부위리스트
			challenge.setBodyList(challengeService.selectBodyPart(challengeId));

			// 완료된 챌린지 일 경우 후기 3개 담아서 주기
			if (challenge.getCheck_date() == 2) {
				reviewList = reviewService.selectThreeReview(challengeId);
			}

			map.put("challenge", challenge);
			map.put("review", reviewList);

			// 후기 총 평점 계산
			List<Review> allReviewList = reviewService.selectAllReview(230);
			int avg_review = 0;
			int size = allReviewList.size();
			if (size != 0) {
				double sum = 0.0;
				for (Review review : allReviewList) {
					sum += review.getStar_point();
				}
				avg_review = (int) Math.round(sum / size);
			}

			map.put("avg_review", avg_review);

		}
		return new ResponseEntity<HashMap<String, Object>>(map, HttpStatus.OK);
	}

	/**
	 * 북마크한 챌린지 리스트 반환
	 */
	@GetMapping("/bookmark")
	public ResponseEntity<List<Challenge>> BookmarkCahllengeList(@RequestParam String uid) {

		List<Challenge> returnList = new ArrayList<Challenge>(); // 반환리스트

		List<Bookmark> bookmark = bookmarkService.selectAllBookmark(uid);

		if (bookmark.size() != 0) {
			for (Bookmark b : bookmark) {
				Challenge ch = challengeRepository.findById(b.getChallenge_id()).get();
				returnList.add(ch);
			}
		}

		return new ResponseEntity<List<Challenge>>(returnList, HttpStatus.OK);
	}

	/**
	 * 메인 챌린지 리스트 반환
	 */
	@GetMapping("/main")
	public ResponseEntity<List<Challenge>> mainChallengeList(@RequestParam int sort) {

		// 캐시 없으면 - 캐시 생성
		if (!redisTemplate.hasKey("challenge")) {
			List<Challenge> list = challengeService.AllChallengeList();
			challengeRepository.saveAll(list);
		}

		// 캐시있다면 - 캐시 뿌림
		List<Challenge> cacheList = (List<Challenge>) challengeRepository.findAll();
		List<Challenge> returnList = new ArrayList<Challenge>(); // 반환 챌린지 리스트

		// 0. 정렬 - 기본값: 최신순 / 0:인기순, 1:최신
		if (sort == 0) {
			Collections.sort(cacheList, new Comparator<Challenge>() {
				@Override
				public int compare(Challenge o1, Challenge o2) {
					return o2.getPeople() - o1.getPeople();
				}
			});
		} else {// 최신순
			Collections.sort(cacheList, new Comparator<Challenge>() {
				@Override
				public int compare(Challenge o1, Challenge o2) {
					return o2.getChallenge_id() - o1.getChallenge_id();
				}
			});

		}

		int size = cacheList.size();
		if (cacheList.size() > 20)
			size = 20;
		for (int i = 0; i < size; i++) {
			returnList.add(cacheList.get(i));
		}

		return new ResponseEntity<List<Challenge>>(returnList, HttpStatus.OK);
	}

	/**
	 * 챌린지 전체리스트 반환
	 * 
	 */
	@GetMapping("/all")
	public ResponseEntity<List<Challenge>> AllChallengeListId(@RequestParam String[] day, @RequestParam int sort,
			@RequestParam int periodStart, @RequestParam int periodEnd, @RequestParam int category) {

		// 캐시 없으면 - 캐시 생성
		if (!redisTemplate.hasKey("challenge")) {
			List<Challenge> list = challengeService.AllChallengeList();
			challengeRepository.saveAll(list);
		}

		// 캐시있다면 - 캐시 뿌림
		List<Challenge> cacheList = (List<Challenge>) challengeRepository.findAll();
		List<Challenge> returnList = new ArrayList<Challenge>(); // 반환 ID 리스트

		// 0. 정렬 - 기본값: 최신순 / 0:인기순, 1:최신
		if (sort == 0) {
			Collections.sort(cacheList, new Comparator<Challenge>() {
				@Override
				public int compare(Challenge o1, Challenge o2) {
					return o2.getPeople() - o1.getPeople();
				}
			});
		} else {// 최신순
			Collections.sort(cacheList, new Comparator<Challenge>() {
				@Override
				public int compare(Challenge o1, Challenge o2) {
					return o2.getChallenge_id() - o1.getChallenge_id();
				}
			});

		}

		// 1. 카테고리별 - 기본값: 0-전체선택 / 해당카테고리 선택.
		if (category != 0) {
			for (Iterator<Challenge> it = cacheList.iterator(); it.hasNext();) {
				Challenge value = it.next();
				if (value.getFit_id() != category) {
					it.remove();
				}
			}
		}

		// 2. 필터 - 기간
		if (periodStart != 0 && periodEnd != 0) {
			for (Iterator<Challenge> it = cacheList.iterator(); it.hasNext();) {
				Challenge value = it.next();
				if (value.getPeriod() < periodStart || value.getPeriod() > periodEnd) {
					it.remove();
				}
			}
		}

		// 3. 필터 - 요일 -> 일단 같은 요일만.
		String param_s = Arrays.toString(day);
		if (day != null && day.length > 0) {
			for (Iterator<Challenge> it = cacheList.iterator(); it.hasNext();) {
				Challenge value = it.next();
				String s = value.getDaylist_string();
				if (!param_s.equals(s)) {
					it.remove();
				}
			}
		}

		// 4. 전체리스트 반환
		for (Challenge ch : cacheList) {
			returnList.add(ch);
		}

		return new ResponseEntity<List<Challenge>>(returnList, HttpStatus.OK);
	}

	/** 챌린지 통계 */
	@GetMapping("/figures/{month}")
	public ResponseEntity<HashMap<String, Object>> figures(@RequestParam String uid, @PathVariable int month) {
		HashMap<String, Object> map = new HashMap<String, Object>();

		// 1. 월별챌린지
		List<UserRate> monthList = challengeService.selectMonthChallenge(uid, month);
		map.put("monthList", monthList);

		// 1-1. 이번달 평균 담아서 주기.
		int current_average_rate = 0;
		int monthCnt = monthList.size();
		double sum = 0.0;
		for (UserRate userRate : monthList) {
			sum += userRate.getAchievement_rate();
		}
		current_average_rate = (int) Math.round((sum / monthCnt)); // 소수 첫번째자리 반올림
		map.put("current_average_rate", current_average_rate);

		// 2. 지난달 나와의 비교 -> 지난달 평균 계산
		int premonth = month - 1;
		int previous_average_rate = 0;
		if (premonth > 0) { // 1월 이전인 12월은 x
			List<UserRate> preList = challengeService.selectMonthChallenge(uid, premonth);
			int premonthCnt = preList.size();
			double presum = 0.0;
			for (UserRate userRate : preList) {
				presum += userRate.getAchievement_rate();
			}
			previous_average_rate = (int) Math.round((presum / premonthCnt)); // 소수 첫번째자리 반올림
		}
		map.put("previous_average_rate", previous_average_rate);

		// 3. 완료된 참여한 운동 카테고리 별 갯수 반환
		int fitList[] = new int[11];
		int dbfitList[] = challengeService.selectFitId(uid);
		for (int i = 0; i < dbfitList.length; i++) {
			int num = dbfitList[i];
			fitList[num]++;
		}
		map.put("categoryList", fitList);

		// 4. 완료된 참여한 부위 별 갯수 반환
		int bodyList[] = new int[10];
		int dbbodyList[] = challengeService.selectBodyId(uid);
		for (int i = 0; i < dbbodyList.length; i++) {
			int num = dbbodyList[i];
			bodyList[num]++;
		}
		map.put("bodyList", bodyList);

		// 5. 다른 사람들과의 비교 top
		// 나이, 연령대 선호하는 운동카테고리 (인기순 정렬)
		// - 회원의 성별, 나이 가져옴
		// -
		// 나이, 연령대 선호하는 부위 카테고리 (인기순 정렬)

		// 6. 나이,연령대 가장 많이 도전한 챌린지 -> 보류
		return new ResponseEntity<HashMap<String, Object>>(map, HttpStatus.OK);
	}

	/**
	 * 인증수 계산 method
	 * 
	 * @throws ParseException
	 */
	static public int certification_day(int[] dayList, String date, int oneCnt, int period) throws ParseException {

		DateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date data = dataFormat.parse(date);
		Calendar cal = Calendar.getInstance();
		cal.setTime(data);

		// int oneCnt = ch.getDay_certify_count();//하루 인증횟수
		int certDay = 0; // 인증요일 수
		int startDayNum = cal.get(Calendar.DAY_OF_WEEK) - 1;// 일정의 시작요일 숫자(ex.월 =1, 수 = 3)
		if (startDayNum == 0)
			startDayNum = 7; // 일요일 0 -> 7

		if (period <= 7) {
			certDay = dayList.length;
		} else {
			int count = 0;
			// int period = ch.getPeriod();
			for (int i = 0; i < dayList.length; ++i) {
				int day = dayList[i];
				if ((startDayNum <= day && day < period % 7 + startDayNum)
						|| ((startDayNum <= day + 7 && day + 7 < period % 7 + startDayNum))) {
					++count;
				}
			}
			certDay = (period / 7 * dayList.length) + count;
		}
		int certification = certDay * oneCnt;
		return certification;
	}

}
