package com.example.example_roomdb

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.example_roomdb.database.AppDatabase
import com.example.example_roomdb.database.DataEntity
import com.example.example_roomdb.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

class MainActivity : AppCompatActivity() {

    // 바인딩 객체 및 데이터베이스 선언
    private lateinit var binding: ActivityMainBinding
    private lateinit var appDatabase: AppDatabase

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Room 데이터베이스 초기화
        appDatabase = AppDatabase.getInstance(this)!!

        // 버튼 클릭 시 데이터 삽입 실행
        binding.insertBtn.setOnClickListener {
            insertData()
            Toast.makeText(this, "데이터가 저장되었습니다.", Toast.LENGTH_SHORT).show()
            Log.v("DB Info","Data Save Successful!")
        }

        // 초기 UI 설정
        initView()
    }

    // 데이터베이스에서 데이터를 가져와 UI에 표시하는 함수
    private fun initView() {
        lifecycleScope.launch(Dispatchers.IO) {
            val temps = appDatabase.dataDao()?.selectAll() // 모든 데이터 조회
            temps?.let {
                for (i in it) {
                    updateView(i) // 각각의 데이터를 UI에 업데이트
                }
            }
        }
        Toast.makeText(this,"정상적으로 데이터 로드됨.", Toast.LENGTH_SHORT).show()
        Log.v("DB Info","Data Load Successful!")
    }

    // 데이터를 삽입하는 함수
    @RequiresApi(Build.VERSION_CODES.O)
    private fun insertData() {
        // 제목 또는 내용이 비어있을 경우 리턴
        if (binding.titleText.text.isNullOrEmpty() || binding.contentText.text.isNullOrEmpty())
            return

        // 데이터 엔티티 생성 (현재 날짜와 시간을 포함)
        val insertTmp = DataEntity(
            0,
            binding.titleText.text.toString(),
            binding.contentText.text.toString(),
            "${LocalDate.now()} ${LocalTime.now().toString().subSequence(0, 8)}"
        )

        // 데이터베이스에 데이터를 비동기로 삽입
        lifecycleScope.launch(Dispatchers.IO) {
            appDatabase.dataDao()?.insert(insertTmp)
            launch(Dispatchers.Main) {
                updateView(insertTmp) // 삽입한 데이터를 UI에 업데이트
            }
        }

        // 입력 필드 초기화 및 키보드 닫기
        binding.titleText.text = null
        binding.contentText.text = null
        closeKeyboard()
    }

    // UI를 업데이트하는 함수 (메인 스레드에서 실행되어야 함)
    private fun updateView(insertData: DataEntity) {
        // 비동기 코루틴 사용, UI 업데이트는 메인 스레드에서 처리
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                // 새로운 레이아웃 생성 및 설정
                val addLayout = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                // 데이터 표시를 위한 TextView 생성
                val textTmp = TextView(this@MainActivity).apply {
                    text = "${insertData.title}\n${insertData.content}\n${insertData.date}"
                    textSize = 24f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        weight = 0.8f
                        setMargins(60, 5, 5, 10)
                    }
                }
                addLayout.addView(textTmp)

                // 삭제 버튼 생성 및 설정
                val deleteTmp = Button(this@MainActivity).apply {
                    text = "삭제" // 버튼에 표시될 텍스트
                    textSize = 18f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT).apply {
                        weight = 0.2f
                    }
                    setPadding(5, 5, 5, 5)
                    setOnClickListener {
                        // 데이터베이스에서 해당 데이터 삭제 및 레이아웃 제거
                        deleteData(insertData.id)
                        binding.masterLayout.removeView(addLayout)
                    }
                }
                addLayout.addView(deleteTmp)

                // 생성한 레이아웃을 메인 레이아웃에 추가
                binding.masterLayout.addView(addLayout)
            }
        }
    }

    // 데이터베이스에서 데이터를 삭제하는 함수
    private fun deleteData(id: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            appDatabase.dataDao()?.deleteById(id) // 데이터 ID를 이용해 삭제
        }
        Toast.makeText(this,"데이터가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
        Log.v("DB Info","Data Delete Successful!")
    }

    // 키보드를 닫는 함수
    private fun closeKeyboard() {
        val view = currentFocus
        if (view != null) {
            val inputMethodManager = getSystemService(InputMethodManager::class.java)
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}
