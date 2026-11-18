package id.ac.polbeng.depandienda.mediastoregson.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import id.ac.polbeng.depandienda.mediastoregson.R
import id.ac.polbeng.depandienda.mediastoregson.data.User
import id.ac.polbeng.depandienda.mediastoregson.data.UserRepository
import id.ac.polbeng.depandienda.mediastoregson.databinding.ActivityMainBinding
import id.ac.polbeng.depandienda.mediastoregson.helper.Config
import id.ac.polbeng.depandienda.mediastoregson.viewmodel.UserViewModel
import id.ac.polbeng.depandienda.mediastoregson.viewmodel.UserViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: UserAdapter
    private var editingUser: User? = null

    private val viewModel: UserViewModel by viewModels {
        UserViewModelFactory(UserRepository(applicationContext))
    }

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            viewModel.exportToUri(it)
            Toast.makeText(this, "Data exported successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            viewModel.importFromUri(it)
            Toast.makeText(this, "Data imported successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        viewModel.loadUsers()
    }

    private fun setupRecyclerView() {
        adapter = UserAdapter(
            onEdit = { user ->
                editingUser = user
                binding.etId.setText(user.id)
                binding.etName.setText(user.name)
                binding.etEmail.setText(user.email)
                binding.btnAdd.text = getString(R.string.update)
            },
            onDelete = { user ->
                viewModel.deleteUser(user)
                Snackbar.make(binding.root, "User deleted", Snackbar.LENGTH_LONG)
                    .setAction("Undo") {
                        viewModel.undoDelete()
                    }.show()
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnAdd.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()

            if (name.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Please enter both name and email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = if (editingUser != null) {
                User(id = editingUser!!.id, name = name, email = email)
            } else {
                User(name = name, email = email)
            }

            viewModel.addOrUpdate(user)
            clearInputs()
        }

        binding.btnExport.setOnClickListener {
            exportLauncher.launch(Config.FILE_NAME)
        }

        binding.btnImport.setOnClickListener {
            importLauncher.launch("*/*")
        }
    }

    private fun observeViewModel() {
        viewModel.users.observe(this) { users ->
            adapter.submitList(users)
        }
    }

    private fun clearInputs() {
        editingUser = null
        binding.etId.setText("")
        binding.etName.setText("")
        binding.etEmail.setText("")
        binding.btnAdd.text = getString(R.string.add)
    }
}
