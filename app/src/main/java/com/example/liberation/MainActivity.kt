package com.example.liberation

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.layout)
        requestDefaultBrowserRole()//기본 브라우저 설정

        val serviceSwitch = findViewById<Switch>(R.id.switch1)//서비스 켜고 끄는 스위치
        val statusTextView = findViewById<TextView>(R.id.statusText)//서비스 상태 표시
        val btnSelectBrowser = findViewById<Button>(R.id.btnSelectBrowser)//이용할 브라우저 선택
        val viewlog = findViewById<LinearLayout>(R.id.btnViewLog)//로그 버튼
        val viewpolicy = findViewById<LinearLayout>(R.id.btnViewPolicy)//처리방침 버튼


        val sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE)//저장된 설정들
        val isEnabled = sharedPref.getBoolean("service_enabled", false)//서비스가 켜저있는지
        val savedPkg = sharedPref.getString("selected_browser_pkg", null)//저장된 브라우저
        val hasAgreed = sharedPref.getBoolean("terms_agreed", false)//이용약관 동의여부(기본값:false)


        if (!hasAgreed) {
            // 동의하지 않았다면 동의 개인정보 수집동의 페이지로 이동
            val intent = Intent(this, TermsActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        serviceSwitch.isChecked = isEnabled//서비스 적용여부 확인
        if (isEnabled) {
            statusTextView.text = "작동중"
            statusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else {
            statusTextView.text = "서비스 꺼짐"
            statusTextView.setTextColor(0xFF909090.toInt())
        }

        //브라우저가 저장된 게 있다면 이름을 찾아 표시, 없으면 기본 문구
        if (savedPkg != null) {
            val browsers = getInstalledBrowsers()
            val currentBrowser = browsers.find { it.packageName == savedPkg }
            btnSelectBrowser.text = "연결 앱: ${currentBrowser?.name ?: "알 수 없음"}"
        } else {
            btnSelectBrowser.text = "클릭하여 브라우저 선택"
        }

        viewlog.setOnClickListener {//로그 화면 불러오기
            val intent = Intent(this, LogActivity::class.java)
            startActivity(intent)
        }
        viewpolicy.setOnClickListener {//처리방침 정책 화면 불러오기
            val intent = Intent(this, PolicyActivity::class.java)
            startActivity(intent)
        }

        btnSelectBrowser.setOnClickListener {//브라우저 선택 팝업
            showBrowserSelectionDialog(btnSelectBrowser)
        }

        serviceSwitch.setOnCheckedChangeListener { _, isChecked ->//서비스 켜고 끄는 이벤트
            val sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE)
            sharedPref.edit().putBoolean("service_enabled", isChecked).apply()
            if (isChecked) {
                statusTextView.text = "작동중"
                statusTextView.setTextColor(getColor(android.R.color.holo_green_dark))
            } else {
                statusTextView.text = "서비스 꺼짐"
                statusTextView.setTextColor(0xFF909090.toInt())
            }
        }

    }




    private val roleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // 사용자가 우리 앱을 기본 앱으로 선택함!
        } else {
            // 사용자가 거절함
        }
    }

    private fun requestDefaultBrowserRole() {//기본 브라우저 설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
                roleLauncher.launch(intent)
            }
        }
    }
    data class BrowserInfo(val name: String, val packageName: String)

    private fun getInstalledBrowsers(): List<BrowserInfo> {//브라우저 리스트를 구하는 함수
        val browsers = mutableListOf<BrowserInfo>()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://"))
        val resolveInfos = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)

        for (info in resolveInfos) {
            val appName = info.loadLabel(packageManager).toString()
            val pkgName = info.activityInfo.packageName

            // 본인 앱은 목록에서 제외
            if (pkgName != packageName) {
                browsers.add(BrowserInfo(appName, pkgName))
            }
        }
        return browsers
    }

    private fun saveSelectedBrowser(packageName: String) {//선택한 브라우저 저장
        val sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("selected_browser_pkg", packageName)
            apply()
        }
    }
    private fun showBrowserSelectionDialog(btnSelectBrowser: Button) {//브라우저 선택 팝업
        val browsers = getInstalledBrowsers()
        val browserNames = browsers.map { it.name }.toTypedArray()

        val builder = AlertDialog.Builder(this)
        builder.setTitle("링크를 열 브라우저를 선택하세요")

        // 목록형 다이얼로그 설정
        builder.setItems(browserNames) { dialog, which ->
            // 사용자가 선택한 아이템 (which가 인덱스)
            val selectedBrowser = browsers[which]

            // 버튼 텍스트를 선택한 브라우저 이름으로 변경
            btnSelectBrowser.text = "연결 앱: ${selectedBrowser.name}"

            // 💡 중요: 선택한 패키지명을 저장 (SharedPreferences 활용)
            saveSelectedBrowser(selectedBrowser.packageName)

            Toast.makeText(this, "${selectedBrowser.name}(으)로 설정되었습니다.", Toast.LENGTH_SHORT).show()
        }

        builder.show()
    }

}