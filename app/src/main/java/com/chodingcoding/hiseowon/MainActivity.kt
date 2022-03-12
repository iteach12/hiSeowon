package com.chodingcoding.hiseowon
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.chodingcoding.hiseowon.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kr.go.neis.api.School
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.text.SimpleDateFormat
import java.time.Month
import java.time.Year
import java.util.*
import java.util.Calendar.*


class MainActivity : AppCompatActivity() {



    private lateinit var binding: ActivityMainBinding


    val weburl = "https://search.naver.com/search.naver?query=날씨"
    val TAG = "Main Activity"
    val TAG_WEATHER_GUESS = "Weather Guess"

    //현재 미세먼지 등급 판단하기 위한 변수
    //미세, 초미세 중 안좋은 등급으로 정해짐.

    //오늘날짜 용
    var year:Int = 0
    var month:Int = 0
    var date:Int = 0


    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        requestedOrientation = SCREEN_ORIENTATION_LANDSCAPE
        setContentView(view)

        val currentTime: Date = Calendar.getInstance().time
        val dateText: String =
            SimpleDateFormat("yyyy년 MM월 dd일 EE요일", Locale.getDefault()).format(currentTime)
        binding.todaywhatday.text = dateText

        //오늘 날짜 구하기
        val cal:Calendar = Calendar.getInstance()
        //현재 년도, 월, 일
        year = cal.get(YEAR)
        month = cal.get(MONTH)
        date = cal.get(DATE)

        Log.e(TAG, date.toString())
        Log.e(TAG, month.toString())
        Log.e(TAG, year.toString())







        val options = BitmapFactory.Options()
        options.inSampleSize = 2 //이미지 사이즈 1/2 로 줄이기
        view.background = BitmapDrawable(
            resources,
            BitmapFactory.decodeResource(resources, R.drawable.realbackground3, options)
        )

        //반복시키
        GlobalScope.launch {    // 2
            while(true){

                val now: Long = System.currentTimeMillis()
                val mDate = Date(now)
                val simpleDate =
                    SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
                val getTime = simpleDate.format(mDate)
                runOnUiThread {
                    binding.currentTime.text = getTime    // 이 곳에 UI작업을 한다
                }

                delay(60000*10) //15분
                WeatherBasic().execute(weburl)
                WeatherNextGuess().execute(weburl)
            }



        }
        //날씨 불러오기
        WeatherBasic().execute(weburl)
        //주간예보 불러오기

        WeatherNextGuess().execute(weburl)
        //급식, 일정 불러오기
        LunchAsyncTask().execute()
    }

    //나이스에서 급식 메뉴와 스케쥴 등을 가져올 수 있습니다.
    //현재는 급식 메뉴만 사용 중입니다.
    //추가로 스케쥴도 고려해보겠습니다만, 대체적으로 나이스 스케쥴을 쓰는 경우가 드물기에...
    inner class LunchAsyncTask: AsyncTask<String, String, String>(){


        override fun doInBackground(vararg p0: String?): String {

            /**
             * 관련 내용은 웹 사이트 참고.
             * 급식과 학사일정이 모두 가능함.
             */

            val school = School(School.Type.ELEMENTARY, School.Region.KANGWON, "K100002451")

            val menu = school.getMonthlyMenu(year, month+1)

            //스케쥴도 담아서 쓸 수는 있지만 아직은 보류중.
            val schduleTemp = school.getMonthlySchedule(year, month+1)
            Log.e(TAG, schduleTemp[date-1].schedule)


            //오늘의 메뉴 리턴
            return menu[date-1].lunch
        }

        override fun onPostExecute(result: String?) {

            Log.e(TAG, result) //확인
            val myResult = result?.replace("[0-9]", "") //숫자 지우기 안되네????
            binding.lunchtextview.text = myResult //급식

        }

    }

    /*
    * 1. 미세먼지
    * 2. 초미세먼지
    * 3. 현재 기온
    * 4. 야외활동 가능 여부
    * 5.
    */
    inner class WeatherBasic: AsyncTask<String, String, String>(){

        /**
         * 여기서 크롤링.
         * 미세먼지, 초미세먼지, 온도, 주간날씨예보 가져오기.
         * 추가로 급식메뉴와 학사일정도 가져오기.
         * 가져온 값을 StringBuffer에 넣어서 결과값으로 던지기
         * post쪽에서 자동으로 값을 받아서 후처리.
         */

        var evaluationGrade:Int = 0

        override fun doInBackground(vararg p0: String?): String {
            val doc: Document = Jsoup.connect("$weburl").get()
            val sb:StringBuffer = StringBuffer()

            //미세먼지(1) 초미세먼지(2) 구하기
            val miseElts: Elements = doc.select(".detail_box").select(".indicator").select("dd").select(".num")
            val todayMise = miseElts[0].text()
            val todayChoMise = miseElts[1].text()

            //온도구하기 todaytemp로 된 값을 다 구하는데, 그 중 첫번째가 온도더라..?
            // 온도찾기.
            val tempratureElts: Elements = doc.select(".todaytemp")
            val myTodayTemp = tempratureElts[0].text() //현재 온도 구해서 string값으로 저장.

            //주간기온 및 강수량 찾기
            val weather1 = doc.select(".table_info.weekly._weeklyWeather").select(".date_info.today").select(".day_info")
            val weather2 = doc.select(".table_info.weekly._weeklyWeather").select(".date_info.today").select(".rain_rate").select(".num")
            val weather3 = doc.select(".table_info.weekly._weeklyWeather").select(".date_info.today").select("dl").select("span")

            //주간날씨 찾기


            val weatherTemp1 = weather1.text()+" "
            val weatherTemp2 = weather2.text()+" "
            val weatherTemp3 = weather3.text()

            //스트링버퍼에 집어넣어서 나중에 스플릿하기.
            sb.append(todayMise)
            sb.append("+")
            sb.append(todayChoMise)
            sb.append("+")
            sb.append(myTodayTemp)
            sb.append("+")
            sb.append("$")
            sb.append(weatherTemp1)
            sb.append(weatherTemp2)
            sb.append(weatherTemp3)



            return sb.toString()
        }

        override fun onPostExecute(result: String?) {

            //미세먼지 스트링 화면용
            val mise = result?.split("+")

            //미세먼지 인트 체크용(나쁨 보통 등)
            val check = result?.split("+", "㎍/㎥")

            //주간날씨 스트링 화면용
            val week = result?.split("$")
            val weekFinal = week?.get(1)?.split(" ")



            //미세먼지+온도 화면 적용
            binding.misetext.text = mise?.get(0)
            binding.chomisetext.text = mise?.get(1)
            binding.temptext.text = mise?.get(2)+"℃"

            //미세먼지 정도 체크하기
            //종종 오류가 생겨 값이 문자 ( - )가 들어가는 경우가 있음.
            //제대로 된 값이 아니면 점검중이라는 문구가 뜨도록 설정함.
            val miseIcon = if(check?.get(0)?.toInt() !is Int) 999 else check[0].toInt()
            val choMiseIcon = if(check!![2] == "-") 999 else check[2].toInt() //split 할 때 빈칸이 생겨서 3번째걸 받았음.




            //이미지 변경 lottie 너무 좋아 진짜로...


            //미세먼지 좋음, 보통, 나쁨, 매우나쁨 체크하기
            when {
                (miseIcon >= 0) and (miseIcon <= 30) -> {
                    binding.misesimpletext.text = "좋음"
                    evaluationGrade += 1
                }
                (miseIcon >= 31) and (miseIcon <= 80) -> {
                    binding.misesimpletext.text = "보통"
                    evaluationGrade += 2
                }
                (miseIcon >= 81) and (miseIcon <= 150) -> {
                    binding.misesimpletext.text = "나쁨"
                    evaluationGrade += 3
                }
                (miseIcon >= 151) and (miseIcon < 900) -> {
                    binding.misesimpletext.text = "매우나쁨"
                    evaluationGrade += 4
                }
                miseIcon == 999 -> {
                    binding.misesimpletext.text = "점검중"
                    evaluationGrade += 900
                }
                else -> {
                    binding.misesimpletext.text = "매우나쁨"
                    evaluationGrade += 4
                }
            }

            //초미세먼지 좋음, 보통, 나쁨, 매우나쁨 체크하기
            when {
                (choMiseIcon >= 0) and (choMiseIcon <= 15) -> {
                    binding.chomisesimpletext.text = "좋음"
                    evaluationGrade += 1
                }
                (choMiseIcon >= 16) and (choMiseIcon <= 35) -> {
                    binding.chomisesimpletext.text = "보통"
                    evaluationGrade += 2
                }
                (choMiseIcon >= 36) and (choMiseIcon <= 75) -> {
                    binding.chomisesimpletext.text = "나쁨"
                    evaluationGrade += 3
                }
                (choMiseIcon >= 76) and (choMiseIcon < 900) -> {
                    binding.chomisesimpletext.text = "매우나쁨"
                    evaluationGrade += 4
                }
                choMiseIcon == 999 -> {
                    binding.chomisesimpletext.text = "점검중"
                    evaluationGrade += 900

                }
                else -> {
                    binding.chomisesimpletext.text = "매우나쁨"
                    evaluationGrade += 4
                }
            }

            when(evaluationGrade){
                2 -> {
                    binding.totalimageView.setAnimation("emotioniconsmile.json")
                    binding.totaltextview.text = "밖에서 놀아도 괜찮아요!"
//                    binding.totalimageView.setAnimation("myempty.json")


                }
                3, 4 -> {

                    if(binding.chomisesimpletext.text == "나쁨" || binding.misesimpletext.text == "나쁨"){
                        binding.totalimageView.setAnimation("crying.json")
                        binding.totaltextview.text = "야외활동은 안돼요!"
                    }else{
                        binding.totalimageView.setAnimation("emotioniconsmile.json")
                        binding.totaltextview.text = "밖에서 놀아도 괜찮아요!"
                    }



                }
                5, 6 -> {
                    binding.totalimageView.setAnimation("crying.json")
                    binding.totaltextview.text = "야외활동은 안돼요!"

                }
                7, 8 -> {
                    binding.totalimageView.setAnimation("crying.json")
                    binding.totaltextview.text = "야외활동은 절대 안돼요!"
                }
                else -> {
                    binding.totalimageView.setAnimation("crying.json")

                }

            }

            //평가등급 적용하기
            binding.totalimageView.playAnimation()
//            binding.totalimageView.scale = 0.8f



            //주간날씨 적용하기
            //첫 번째
            binding.whatday.text = weekFinal?.get(0)
            binding.today.text = weekFinal?.get(1)
            binding.rainpercent11.text = weekFinal?.get(20)
            binding.weatherimageview11.scale = 0.1f
            binding.rainpercent12.text = weekFinal?.get(21)
            binding.weatherimageview12.scale = 0.1f

            //두 번째
            binding.whatday2.text = weekFinal?.get(2)
            binding.tommorow.text = weekFinal?.get(3)
            binding.rainpercent21.text = weekFinal?.get(22)
            binding.weatherimageview21.scale = 0.1f
            binding.rainpercent22.text = weekFinal?.get(23)
            binding.weatherimageview22.scale = 0.1f

            //세 번째
            binding.whatday3.text = weekFinal?.get(4)
            binding.nexttomorrow.text = weekFinal?.get(5)
            binding.rainpercent31.text = weekFinal?.get(24)
            binding.weatherimageview31.scale = 0.1f
            binding.rainpercent32.text = weekFinal?.get(25)
            binding.weatherimageview32.scale = 0.1f

            //네 번째
            binding.whatday4.text = weekFinal?.get(6)
            binding.nextnexttomorrow.text = weekFinal?.get(7)
            binding.rainpercent41.text = weekFinal?.get(26)
            binding.weatherimageview41.scale = 0.1f
            binding.rainpercent42.text = weekFinal?.get(27)
            binding.weatherimageview42.scale = 0.1f

            //다섯 번째
            binding.whatday5.text = weekFinal?.get(8)
            binding.nextnextnexttomorrow.text = weekFinal?.get(9)
            binding.rainpercent51.text = weekFinal?.get(28)
            binding.weatherimageview51.scale = 0.1f
            binding.rainpercent52.text = weekFinal?.get(29)
            binding.weatherimageview52.scale = 0.1f

        }




    }


    /*
    * 1. 주간 강수 예보 (오전, 오후 강수확률)
    * 2. 주간 날씨 예보 (맑음, 흐림, 비, 눈 등)
    * 3. 5일치를 보여줍니다.
    * */
    inner class WeatherNextGuess: AsyncTask<String, String, String>(){

        override fun doInBackground(vararg p0: String?): String {

            val doc: Document = Jsoup.connect("$weburl").get()
            val sb:StringBuffer = StringBuffer()
            val weatherGuessMorning:Elements = doc.select(".table_info.weekly._weeklyWeather").select(".date_info.today").select(".point_time.morning").select(".ico_state2")
            val weatherGuessAfternoon:Elements = doc.select(".table_info.weekly._weeklyWeather").select(".date_info.today").select(".point_time.afternoon").select(".ico_state2")
            val morningGuessList:List<String> = weatherGuessMorning.eachAttr("class")
            val afterNoonGuessList:List<String> = weatherGuessAfternoon.eachAttr("class")

            for(i in morningGuessList){
                val new_i =i.replace("ico_state2", "").replace(" ", "")
                sb.append(new_i+"+")
            }

            for(i in afterNoonGuessList){
                val new_i = i.replace("ico_state2", "").replace(" ", "")

                sb.append(new_i+"+")
            }




            return sb.toString()

        }
        override fun onPostExecute(result: String?) {


            val weatherGuessFinalResult = result?.split("+")
            Log.e(TAG_WEATHER_GUESS, weatherGuessFinalResult.toString())

            binding.weatherimageview11.setAnimation(matchingWeather(weatherGuessFinalResult!![0]))
            binding.weatherimageview11.playAnimation()

            binding.weatherimageview21.setAnimation(matchingWeather(weatherGuessFinalResult!![1]))
            binding.weatherimageview21.playAnimation()

            binding.weatherimageview31.setAnimation(matchingWeather(weatherGuessFinalResult!![2]))
            binding.weatherimageview31.playAnimation()

            binding.weatherimageview41.setAnimation(matchingWeather(weatherGuessFinalResult!![3]))
            binding.weatherimageview41.playAnimation()

            binding.weatherimageview51.setAnimation(matchingWeather(weatherGuessFinalResult!![4]))
            binding.weatherimageview51.playAnimation()

            binding.weatherimageview12.setAnimation(matchingWeather(weatherGuessFinalResult!![10]))
            binding.weatherimageview12.playAnimation()

            binding.weatherimageview22.setAnimation(matchingWeather(weatherGuessFinalResult!![11]))
            binding.weatherimageview22.playAnimation()

            binding.weatherimageview32.setAnimation(matchingWeather(weatherGuessFinalResult!![12]))
            binding.weatherimageview32.playAnimation()

            binding.weatherimageview42.setAnimation(matchingWeather(weatherGuessFinalResult!![13]))
            binding.weatherimageview42.playAnimation()

            binding.weatherimageview52.setAnimation(matchingWeather(weatherGuessFinalResult!![14]))
            binding.weatherimageview52.playAnimation()

        }

        fun matchingWeather(value:String):String{
            val result = when(value){
                "ws1" -> "weathersunny.json"
                "ws22" -> "weatherpartlyshower.json"
                "ws23" -> "weathersnowsunny.json"
                "ws21" -> "weathersnow.json"
                "ws5" -> "weatherpartlycloudy.json"
                else -> "weathermist.json"
            }

            return result
        }

    }



}
