package com.nur.app.ui.admin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Timestamp
import com.nur.app.data.model.Post
import com.nur.app.data.model.PostType
import com.nur.app.data.repository.FirebaseRepository
import com.nur.app.databinding.ActivityAdminBinding
import com.nur.app.util.hide
import com.nur.app.util.loadUrl
import com.nur.app.util.show
import com.nur.app.util.toast
import kotlinx.coroutines.launch
import java.util.Calendar

class AdminActivity : AppCompatActivity() {

    private lateinit var b: ActivityAdminBinding
    private var selectedImageUri: Uri? = null
    private var scheduledTimestamp: Timestamp? = null
    private var editingPost: Post? = null
    private lateinit var adminAdapter: AdminPostAdapter

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            b.ivImagePreview.show()
            b.ivImagePreview.loadUrl(it.toString())
            b.btnRemoveImage.show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(b.root)
        setSupportActionBar(b.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Admin Panel"

        setupSpinner()
        setupForm()
        setupTabs()
        setupList()
    }

    private fun setupSpinner() {
        val types = PostType.values().map { "${it.emoji} ${it.label}" }
        b.spinnerType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
    }

    private fun setupForm() {
        b.btnPickImage.setOnClickListener { imagePicker.launch("image/*") }
        b.btnRemoveImage.setOnClickListener {
            selectedImageUri = null
            b.ivImagePreview.hide(); b.btnRemoveImage.hide()
        }
        b.btnSchedule.setOnClickListener { showDateTimePicker() }
        b.btnClearSchedule.setOnClickListener {
            scheduledTimestamp = null
            b.tvScheduledTime.text = "Not scheduled — publish immediately"
            b.btnClearSchedule.hide()
        }
        b.btnPublish.setOnClickListener { submitPost() }
        b.btnReset.setOnClickListener   { resetForm() }
    }

    private fun setupTabs() {
        b.tabLayout.addTab(b.tabLayout.newTab().setText("Create Post"))
        b.tabLayout.addTab(b.tabLayout.newTab().setText("Manage Posts"))
        b.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { b.formContainer.show(); b.recyclerAdminPosts.hide() }
                    1 -> { b.formContainer.hide(); b.recyclerAdminPosts.show(); loadPosts() }
                }
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun setupList() {
        adminAdapter = AdminPostAdapter(
            onEdit   = { loadPostIntoForm(it) },
            onDelete = { confirmDelete(it) },
            onTogglePublish = { post ->
                lifecycleScope.launch {
                    FirebaseRepository.publishScheduledPost(post.id)
                    toast("Published!"); loadPosts()
                }
            }
        )
        b.recyclerAdminPosts.layoutManager = LinearLayoutManager(this)
        b.recyclerAdminPosts.adapter = adminAdapter
    }

    private fun loadPosts() {
        lifecycleScope.launch {
            b.progressAdmin.show()
            adminAdapter.submitList(FirebaseRepository.getAllPostsAdmin())
            b.progressAdmin.hide()
        }
    }

    private fun loadPostIntoForm(post: Post) {
        editingPost = post
        b.tabLayout.getTabAt(0)?.select()
        b.spinnerType.setSelection(PostType.values().indexOfFirst { it.name == post.type.name })
        b.etTitle.setText(post.title)
        b.etBody.setText(post.body)
        b.etCategory.setText(post.category)
        b.etTags.setText(post.tags.joinToString(", "))
        post.imageUrl?.let { b.ivImagePreview.show(); b.ivImagePreview.loadUrl(it); b.btnRemoveImage.show() }
        b.btnPublish.text = "Update Post"
        b.tvFormTitle.text = "Edit Post"
    }

    private fun submitPost() {
        val title    = b.etTitle.text.toString().trim()
        val body     = b.etBody.text.toString().trim()
        val category = b.etCategory.text.toString().trim()
        val tags     = b.etTags.text.toString().trim().split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val postType = PostType.values()[b.spinnerType.selectedItemPosition]
        val sendNotif = b.switchNotification.isChecked

        if (title.isEmpty()) { toast("Title is required"); return }
        if (body.isEmpty())  { toast("Body is required");  return }

        b.btnPublish.isEnabled = false; b.progressForm.show()

        lifecycleScope.launch {
            val authorName = FirebaseRepository.getCurrentUser()?.displayName ?: "Admin"
            val result = if (editingPost != null) {
                FirebaseRepository.updatePost(editingPost!!.id, title, body, category, tags, selectedImageUri)
            } else {
                FirebaseRepository.createPost(postType, title, body, category, tags, selectedImageUri, scheduledTimestamp, authorName).map { }
            }
            b.progressForm.hide(); b.btnPublish.isEnabled = true
            result.fold(
                onSuccess = {
                    toast(if (editingPost != null) "Post updated! ✓" else "Post published! ✓")
                    if (sendNotif && editingPost == null) {
                        FirebaseRepository.saveNotificationRecord("New ${postType.label}: $title", body.take(120), null)
                    }
                    resetForm()
                },
                onFailure = { toast("Error: ${it.localizedMessage}") }
            )
        }
    }

    private fun confirmDelete(post: Post) {
        AlertDialog.Builder(this)
            .setTitle("Delete Post")
            .setMessage("Delete \"${post.title}\"? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    FirebaseRepository.deletePost(post.id).fold(
                        onSuccess = { toast("Deleted"); loadPosts() },
                        onFailure = { toast("Error: ${it.localizedMessage}") }
                    )
                }
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun showDateTimePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            TimePickerDialog(this, { _, h, min ->
                cal.set(y, m, d, h, min, 0)
                scheduledTimestamp = Timestamp(cal.time)
                b.tvScheduledTime.text = "Scheduled: ${android.text.format.DateFormat.format("MMM dd, yyyy 'at' hh:mm a", cal)}"
                b.btnClearSchedule.show()
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun resetForm() {
        editingPost = null; selectedImageUri = null; scheduledTimestamp = null
        b.etTitle.text?.clear(); b.etBody.text?.clear()
        b.etCategory.text?.clear(); b.etTags.text?.clear()
        b.spinnerType.setSelection(0)
        b.ivImagePreview.hide(); b.btnRemoveImage.hide()
        b.tvScheduledTime.text = "Not scheduled — publish immediately"
        b.btnClearSchedule.hide(); b.switchNotification.isChecked = false
        b.btnPublish.text = "Publish Post"; b.tvFormTitle.text = "New Post"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
