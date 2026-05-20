package com.example.eticketing

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.eticketing.activities.LoginActivity
import com.example.eticketing.activities.ProfilActivity
import com.example.eticketing.activities.RequestPengelolaActivity
import com.example.eticketing.data.SessionManager
import com.example.eticketing.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cek session expired sebelum apapun
        if (SessionManager.isLoggedIn(this) && SessionManager.isSessionExpired(this)) {
            SessionManager.logout(this, LoginActivity::class.java)
            return
        }
        SessionManager.resetCloseTime(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val role = getSharedPreferences("session", Context.MODE_PRIVATE)
            .getString("userRole", "user")

        setupNavigation(role)
        setupSidebar(role)
    }

    // Reload sidebar setiap kembali ke MainActivity
    override fun onResume() {
        super.onResume()
        val role = getSharedPreferences("session", Context.MODE_PRIVATE)
            .getString("userRole", "user")
        updateSidebarData(role)
    }

    private fun setupNavigation(role: String?) {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNav.menu.clear()
        when (role) {
            "admin" -> {
                navController.setGraph(R.navigation.nav_graph_admin)
                binding.bottomNav.inflateMenu(R.menu.bottom_nav_admin)
            }
            "pengelola" -> {
                navController.setGraph(R.navigation.nav_graph_pengelola)
                binding.bottomNav.inflateMenu(R.menu.bottom_nav_pengelola)
            }
            else -> {
                navController.setGraph(R.navigation.nav_graph_user)
                binding.bottomNav.inflateMenu(R.menu.bottom_nav_user)
            }
        }
        binding.bottomNav.setupWithNavController(navController)
    }

    private fun setupSidebar(role: String?) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar,
            R.string.drawer_open, R.string.drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Icon sidebar pakai warna asli, teks hitam
        binding.navView.itemIconTintList = null
        binding.navView.itemTextColor = ColorStateList.valueOf(Color.parseColor("#333333"))

        // Warnai teks Logout jadi merah
        val logoutItem = binding.navView.menu.findItem(R.id.nav_side_logout)
        logoutItem?.let {
            val s = SpannableString(it.title)
            s.setSpan(ForegroundColorSpan(Color.RED), 0, s.length, 0)
            it.title = s
        }

        updateSidebarData(role)

        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_side_home ->
                    binding.bottomNav.selectedItemId = R.id.nav_home
                R.id.nav_side_destinasi ->
                    binding.bottomNav.selectedItemId = R.id.nav_destinasi
                R.id.nav_side_pesan ->
                    binding.bottomNav.selectedItemId = R.id.nav_pesan
                R.id.nav_side_profil ->
                    startActivity(Intent(this, ProfilActivity::class.java))
                R.id.nav_side_kelola ->
                    binding.bottomNav.selectedItemId = R.id.nav_kelola
                R.id.nav_side_destinasi_saya ->
                    binding.bottomNav.selectedItemId = R.id.nav_destinasi_saya
                R.id.nav_side_kelola_tiket ->
                    binding.bottomNav.selectedItemId = R.id.nav_tiket
                R.id.nav_side_request ->
                    startActivity(Intent(this, RequestPengelolaActivity::class.java))
                R.id.nav_side_logout ->
                    SessionManager.logout(this, LoginActivity::class.java)
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun updateSidebarData(role: String?) {
        val header = binding.navView.getHeaderView(0)
        val ivPhoto = header.findViewById<ImageView>(R.id.ivSidebarPhoto)
        val tvAvatar = header.findViewById<TextView>(R.id.tvAvatarDefault)
        val tvNama = header.findViewById<TextView>(R.id.tvSidebarNama)
        val tvRole = header.findViewById<TextView>(R.id.tvSidebarRole)

        val prefs = getSharedPreferences("session", Context.MODE_PRIVATE)
        val nama = prefs.getString("userName", "-")
        val photoPath = prefs.getString("userPhoto", null)

        tvNama.text = nama
        tvRole.text = role?.replaceFirstChar { it.uppercase() }

        // Set huruf pertama nama sebagai default avatar
        tvAvatar?.text = nama?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

        if (!photoPath.isNullOrEmpty()) {
            val imgFile = File(photoPath)
            if (imgFile.exists()) {
                // Ada foto — sembunyikan avatar default, tampilkan foto
                tvAvatar?.visibility = View.GONE
                ivPhoto?.visibility = View.VISIBLE
                Glide.with(this)
                    .load(imgFile)
                    .circleCrop()
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(ivPhoto!!)
            } else {
                // File tidak ada — tampilkan avatar default
                tvAvatar?.visibility = View.VISIBLE
                ivPhoto?.visibility = View.GONE
            }
        } else {
            // Belum ada foto — tampilkan avatar default
            tvAvatar?.visibility = View.VISIBLE
            ivPhoto?.visibility = View.GONE
        }

        // Visibility menu sidebar sesuai role
        val navMenu = binding.navView.menu
        navMenu.findItem(R.id.nav_side_home)?.isVisible = true
        navMenu.findItem(R.id.nav_side_destinasi)?.isVisible = true
        navMenu.findItem(R.id.nav_side_pesan)?.isVisible = true
        navMenu.findItem(R.id.nav_side_profil)?.isVisible = true
        navMenu.findItem(R.id.nav_side_logout)?.isVisible = true
        navMenu.findItem(R.id.nav_side_kelola)?.isVisible = role == "admin"
        navMenu.findItem(R.id.nav_side_request)?.isVisible = role == "admin"
        navMenu.findItem(R.id.nav_side_destinasi_saya)?.isVisible = role == "pengelola"
        navMenu.findItem(R.id.nav_side_kelola_tiket)?.isVisible = role == "pengelola"
    }

    // Simpan waktu close saat app di-background
    override fun onStop() {
        super.onStop()
        if (SessionManager.isLoggedIn(this)) {
            SessionManager.saveCloseTime(this)
        }
    }

    // Reset timer saat app aktif kembali
    override fun onStart() {
        super.onStart()
        if (SessionManager.isLoggedIn(this) && SessionManager.isSessionExpired(this)) {
            SessionManager.logout(this, LoginActivity::class.java)
            return
        }
        SessionManager.resetCloseTime(this)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
