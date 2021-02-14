package com.ssafy.homfit.model;

import java.io.Serializable;
import java.sql.Date;
import java.util.Arrays;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

/** 챌린지 정보 */

@RedisHash("challenge")
public class Challenge implements Serializable{
	
	@Id
	private int challenge_id;
	private String challenge_title;
	private String challenge_contents;
	private String challenge_img;
	private String challenge_certify_contents;
	private String good_img;
	private String bad_img;
	private int day_certify_count;
	private int only_cam;
	private String start_date;
	private String end_date;
	private String make_date;
	private String make_uid;
	private int fit_id;
	private int check_date;
	
	private int period;
	private String nick_name; //개설자 닉네임
	private int people; //참여자수
	private int kind; //종류 1 - 운동, 2 - 식단
	private int certification; //인증갯수
	private int average_rate; //평균 달성률
	
	private MultipartFile goodImgFile;
	private MultipartFile badImgFile;
	private MultipartFile challengeImgFile;
	
	//요일리스트 스트링
	private String daylist_string;
	//body리스트 스트링
	private String bodylist_string;
	
	//요일리스트
	private int[] dayList;
	//태그리스트
	private String[] tagList;
	//부위리스트
	private int[] bodyList;


	
	public int getChallenge_id() {
		return challenge_id;
	}
	public void setChallenge_id(int challenge_id) {
		this.challenge_id = challenge_id;
	}
	public String getChallenge_title() {
		return challenge_title;
	}
	public void setChallenge_title(String challenge_title) {
		this.challenge_title = challenge_title;
	}
	public String getChallenge_contents() {
		return challenge_contents;
	}
	public void setChallenge_contents(String challenge_contents) {
		this.challenge_contents = challenge_contents;
	}
	public String getChallenge_img() {
		return challenge_img;
	}
	public void setChallenge_img(String challenge_img) {
		this.challenge_img = challenge_img;
	}
	public String getChallenge_certify_contents() {
		return challenge_certify_contents;
	}
	public void setChallenge_certify_contents(String challenge_certify_contents) {
		this.challenge_certify_contents = challenge_certify_contents;
	}
	public String getGood_img() {
		return good_img;
	}
	public void setGood_img(String good_img) {
		this.good_img = good_img;
	}
	public String getBad_img() {
		return bad_img;
	}
	public void setBad_img(String bad_img) {
		this.bad_img = bad_img;
	}
	public int getDay_certify_count() {
		return day_certify_count;
	}
	public void setDay_certify_count(int day_certify_count) {
		this.day_certify_count = day_certify_count;
	}
	public int getOnly_cam() {
		return only_cam;
	}
	public void setOnly_cam(int only_cam) {
		this.only_cam = only_cam;
	}
	public String getStart_date() {
		return start_date;
	}
	public void setStart_date(String start_date) {
		this.start_date = start_date;
	}
	public String getEnd_date() {
		return end_date;
	}
	public void setEnd_date(String end_date) {
		this.end_date = end_date;
	}
	public String getMake_date() {
		return make_date;
	}
	public void setMake_date(String make_date) {
		this.make_date = make_date;
	}
	public String getMake_uid() {
		return make_uid;
	}
	public void setMake_uid(String make_uid) {
		this.make_uid = make_uid;
	}
	public int getFit_id() {
		return fit_id;
	}
	public void setFit_id(int fit_id) {
		this.fit_id = fit_id;
	}
	public int getCheck_date() {
		return check_date;
	}
	public void setCheck_date(int check_date) {
		this.check_date = check_date;
	}
	public int getPeriod() {
		return period;
	}
	public void setPeriod(int period) {
		this.period = period;
	}
	public int[] getDayList() {
		return dayList;
	}
	public void setDayList(int[] dayList) {
		this.dayList = dayList;
	}
	public String[] getTagList() {
		return tagList;
	}
	public void setTagList(String[] tagList) {
		this.tagList = tagList;
	}
	public int[] getBodyList() {
		return bodyList;
	}
	public void setBodyList(int[] bodyList) {
		this.bodyList = bodyList;
	}
	
	public String getDaylist_string() {
		return daylist_string;
	}
	public void setDaylist_string(String daylist_string) {
		this.daylist_string = daylist_string;
	}
	
	public String getNick_name() {
		return nick_name;
	}
	public void setNick_name(String nick_name) {
		this.nick_name = nick_name;
	}
	
	public int getPeople() {
		return people;
	}
	public void setPeople(int people) {
		this.people = people;
	}
	
	public int getKind() {
		return kind;
	}
	public void setKind(int kind) {
		this.kind = kind;
	}
	
	public int getCertification() {
		return certification;
	}
	public void setCertification(int certification) {
		this.certification = certification;
	}
	public int getAverage_rate() {
		return average_rate;
	}
	public void setAverage_rate(int average_rate) {
		this.average_rate = average_rate;
	}
	
	public MultipartFile getGoodImgFile() {
		return goodImgFile;
	}
	public void setGoodImgFile(MultipartFile goodImgFile) {
		this.goodImgFile = goodImgFile;
	}
	public MultipartFile getBadImgFile() {
		return badImgFile;
	}
	public void setBadImgFile(MultipartFile badImgFile) {
		this.badImgFile = badImgFile;
	}
	public MultipartFile getChallengeImgFile() {
		return challengeImgFile;
	}
	public void setChallengeImgFile(MultipartFile challengeImgFile) {
		this.challengeImgFile = challengeImgFile;
	}
	public String getBodylist_string() {
		return bodylist_string;
	}
	public void setBodylist_string(String bodylist_string) {
		this.bodylist_string = bodylist_string;
	}
	@Override
	public String toString() {
		return "Challenge [challenge_id=" + challenge_id + ", challenge_title=" + challenge_title
				+ ", challenge_contents=" + challenge_contents + ", challenge_img=" + challenge_img
				+ ", challenge_certify_contents=" + challenge_certify_contents + ", good_img=" + good_img + ", bad_img="
				+ bad_img + ", day_certify_count=" + day_certify_count + ", only_cam=" + only_cam + ", start_date="
				+ start_date + ", end_date=" + end_date + ", make_date=" + make_date + ", make_uid=" + make_uid
				+ ", fit_id=" + fit_id + ", check_date=" + check_date + ", period=" + period + ", nick_name="
				+ nick_name + ", people=" + people + ", kind=" + kind + ", certification=" + certification
				+ ", average_rate=" + average_rate + ", goodImgFile=" + goodImgFile + ", badImgFile=" + badImgFile
				+ ", challengeImgFile=" + challengeImgFile + ", daylist_string=" + daylist_string + ", bodylist_string="
				+ bodylist_string + ", dayList=" + Arrays.toString(dayList) + ", tagList=" + Arrays.toString(tagList)
				+ ", bodyList=" + Arrays.toString(bodyList) + "]";
	}
	
	
	

	
}
