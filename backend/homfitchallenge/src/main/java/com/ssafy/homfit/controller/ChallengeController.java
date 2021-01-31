package com.ssafy.homfit.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.homfit.model.Challenge;
import com.ssafy.homfit.model.Tag;
import com.ssafy.homfit.model.service.ChallengeService;
import com.ssafy.homfit.model.service.TagService;

@RestController
@RequestMapping("/challenge")
public class ChallengeController {

	private static final Logger logger = LoggerFactory.getLogger(ChallengeController.class);
	private static final String SUCCESS = "success";
	private static final String FAIL = "fail";

	@Autowired
	ChallengeService challengeService;

	@Autowired
	TagService tagService;


	/** 챌린지 참여 */
	@PostMapping("/join/{challengeId}")
	@Transactional
	public ResponseEntity<String> joinChallenge(@PathVariable int challengeId, @RequestBody String uid) {
		if (challengeService.joinChallenge(challengeId, uid)) {
			return new ResponseEntity<String>(SUCCESS, HttpStatus.OK);
		}
		return new ResponseEntity<String>(FAIL, HttpStatus.NO_CONTENT);
	}

	/** 챌린지 참여 삭제 -> 참여자 일때만, 개설자는 챌린지 삭제로 가야함 */
	@DeleteMapping("/join/{challengeId}")
	@Transactional
	public ResponseEntity<String> quitChallenge(@PathVariable int challengeId, @RequestBody String uid) {

		if (challengeService.quitChallenge(challengeId, uid)) {
			return new ResponseEntity<String>(SUCCESS, HttpStatus.OK);
		}
		return new ResponseEntity<String>(FAIL, HttpStatus.NO_CONTENT);
	}

	/** 챌린지 등록 */
	@PostMapping
	@Transactional
	public ResponseEntity<String> insertChallenge(@RequestBody Challenge challenge) {

		HttpStatus status = HttpStatus.OK;
		String result = SUCCESS;

		try {
			
			int kind = challenge.getKind(); //챌린지 종류  -> 1-운동, 2-식단
			int dayList[] = challenge.getDayList();// 요일
			String tagList[] = challenge.getTagList();// 태그
			int bodyList[] = challenge.getBodyList();// 부위
			
			if ( kind == 0 || dayList == null || dayList.length == 0) { //*종류와 요일은 필수값
				result = FAIL;
				throw new Exception();
			} else {
				if(kind == 1 && (bodyList == null || bodyList.length == 0)) {//*운동이라면 부위선택 필수
					result = FAIL;
					throw new Exception();
				}
				challenge.setDaylist_string(Arrays.toString(dayList));
				challengeService.writeChallenge(challenge);
				int challengeId = challenge.getChallenge_id();

				// 1. 요일처리 - 동적쿼리
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("challenge_id", challengeId);
				map.put("list", IntStream.of(dayList).boxed().collect(Collectors.toList()));
				challengeService.writeChallengeDay(map);

				// 2. 부위처리 (운동일때만)
				if(kind == 1) {
					for (int i = 0; i < bodyList.length; i++) {
						HashMap<String, Integer> map_body = new HashMap<String, Integer>();
						map_body.put("challenge_id", challengeId);
						map_body.put("body_id", bodyList[i]);
						challengeService.writeChallengeBody(map_body);
					}
				}

				// 3. 태그처리 - 입력받았을 경우만
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
						tagService.writeTagInChallenge(map_tag); //// tag in challenge
					}
				}

				// 4. 개설자는 참여테이블에 바로 insert
				challengeService.joinChallenge(challengeId, challenge.getMake_uid());
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
	@PutMapping
	@Transactional
	public ResponseEntity<String> updateChallenge(@RequestBody Challenge challenge) {
		
		// null이 아니면 수정 고.
		//HashMap으로 받아서 - 챌린지
		//태그
		//부위 
		
		if (challengeService.updateChallenge(challenge)) {
			return new ResponseEntity<String>(SUCCESS, HttpStatus.OK);
		}
		return new ResponseEntity<String>(FAIL, HttpStatus.NO_CONTENT);

	}

	/** 챌린지 삭제 - 태그테이블은 연쇄삭제 x */
	@DeleteMapping("{challengeId}")
	@Transactional
	public ResponseEntity<String> deleteChallenge(@PathVariable int challengeId) {
		if (challengeService.deleteChallenge(challengeId)) {
			return new ResponseEntity<String>(SUCCESS, HttpStatus.OK);
		}
		return new ResponseEntity<String>(FAIL, HttpStatus.NO_CONTENT);
	}

	/** 챌린지 상세보기 */
	@GetMapping("{challengeId}")
	public ResponseEntity<Challenge> detailChallenge(@PathVariable int challengeId) {

		Challenge challenge = challengeService.detailChallenge(challengeId);
		if (challenge == null) {
			return new ResponseEntity<Challenge>(challenge, HttpStatus.NO_CONTENT);
		} else {
			// 태그리스트
			Tag tag[] = tagService.selectTagInChallenge(challengeId);
			System.out.println(Arrays.toString(tag));
			if (tag.length != 0) {
				String[] taglist = new String[tag.length];
				for (int i = 0; i < tag.length; i++) {
					taglist[i] = tag[i].getTag_name();
				}
				challenge.setTagList(taglist);
			}
			// 부위리스트
			challenge.setBodyList(challengeService.selectBodyPart(challengeId));
		}
		return new ResponseEntity<Challenge>(challenge, HttpStatus.OK);
	}

	/** 챌린지 전체리스트 반환 - 전체, 카테고리별, 필터적용등 */ 
	@GetMapping("/all")
	public ResponseEntity<Challenge[]> AllChallengeList() {
		// 대표이미지, 챌린지 제목, 개설자, 개설자이미지, 인증빈도(월화수목금), 기간, 참여중 인원

		Challenge[] list = challengeService.AllChallengeList();
		Challenge[] people = challengeService.selectParticipants();

		while (people.length != list.length) { // 둘의 길이가 같지 않다 => 도중에 crud일어났을 수도 있음
			list = challengeService.AllChallengeList();
			people = challengeService.selectParticipants();
		}

		for (int i = 0; i < list.length; i++) {
			if (list[i].getChallenge_id() == people[i].getChallenge_id()) {
				list[i].setPeople(people[i].getPeople());
			}
		}

		// 인기순, 신규순 필터 적용

		return new ResponseEntity<Challenge[]>(list, HttpStatus.OK);
	}

}
