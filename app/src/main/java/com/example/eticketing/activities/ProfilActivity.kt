package com.example.eticketing.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.eticketing.data.AppDatabase
import com.example.eticketing.data.SessionManager
import com.example.eticketing.databinding.ActivityProfilBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ProfilActivity : BaseActivity() {

    private lateinit var binding: ActivityProfilBinding

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val path = copyImageToInternal(uri)
                if (path != null) savePhoto(path)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfilBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBackButton("Profil Saya")

        val userId = SessionManager.getUserId(this)
        val db = AppDatabase.getDatabase(this)

        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) { db.userDao().getUserById(userId) }
            user?.let {
                binding.tvNama.text = it.nama
                binding.tvEmail.text = it.email
                binding.tvRole.text = it.role.replaceFirstChar { c -> c.uppercase() }
                binding.etWhatsapp.setText(it.whatsapp ?: "")

                // Set default avatar huruf pertama nama
                val firstChar = it.nama.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                binding.tvAvatarDefault.text = firstChar

                if (!it.photoPath.isNullOrEmpty()) {
                    // Tampilkan foto, sembunyikan avatar default
                    binding.tvAvatarDefault.visibility = View.GONE
                    binding.ivFotoProfil.visibility = View.VISIBLE
                    Glide.with(this@ProfilActivity)
                        .load(File(it.photoPath))
                        .circleCrop()  // ← ini yang bikin bulat sempurna
                        .into(binding.ivFotoProfil)
                } else {
                    binding.tvAvatarDefault.visibility = View.VISIBLE
                    binding.ivFotoProfil.visibility = View.GONE
                }
            }
        }

        binding.btnGantiFoto.setOnClickListener { openGallery() }
        binding.ivFotoProfil.setOnClickListener { openGallery() }
        binding.tvAvatarDefault.setOnClickListener { openGallery() }

        binding.btnSimpanWa.setOnClickListener {
            val wa = binding.etWhatsapp.text.toString().trim()
            if (wa.isEmpty()) {
                Toast.makeText(this, "Nomor WA tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                withContext(Dispatchers.IO) { db.userDao().updateWhatsapp(userId, wa) }
                Toast.makeText(this@ProfilActivity, "Nomor WA berhasil disimpan", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnLogout.setOnClickListener {
            SessionManager.logout(this, LoginActivity::class.java)
        }
    }

    private fun savePhoto(path: String) {
        val userId = SessionManager.getUserId(this)
        val db = AppDatabase.getDatabase(this)

        lifecycleScope.launch {
            withContext(Dispatchers.IO) { db.userDao().updatePhoto(userId, path) }

            // Simpan path ke session supaya sidebar bisa load
            getSharedPreferences("session", MODE_PRIVATE)
                .edit().putString("userPhoto", path).apply()

            // Tampilkan foto bulat
            binding.tvAvatarDefault.visibility = View.GONE
            binding.ivFotoProfil.visibility = View.VISIBLE
            Glide.with(this@ProfilActivity)
                .load(File(path))
                .circleCrop()
                .into(binding.ivFotoProfil)

            Toast.makeText(this@ProfilActivity, "Foto profil berhasil diubah", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Intent(MediaStore.ACTION_PICK_IMAGES)
        } else {
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        }
        pickImageLauncher.launch(intent)
    }

    private fun copyImageToInternal(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val dir = File(filesDir, "profiles")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "profile_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out -> inputStream.copyTo(out) }
            inputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}