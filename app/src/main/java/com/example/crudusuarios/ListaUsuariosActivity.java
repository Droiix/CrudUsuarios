package com.example.crudusuarios;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ListaUsuariosActivity extends AppCompatActivity {
    ListView listUsuarios;
    ArrayAdapter<String> adapter;
    ArrayList<String> lista;
    ArrayList<Usuario> usuarios;
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_usuarios);
        listUsuarios = findViewById(R.id.listUsuarios);
        dbHelper = new DBHelper(this);
        cargarUsuarios();

        listUsuarios.setOnItemClickListener((parent, view, position, id) -> {
            if (usuarios != null && position < usuarios.size()) {
                Usuario usuario = usuarios.get(position);
                mostrarDialogo(usuario);
            }
        });
    }

    private void cargarUsuarios() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            ArrayList<Usuario> tempUsuarios = new ArrayList<>();
            ArrayList<String> tempLista = new ArrayList<>();
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM usuarios", null);

            while(cursor.moveToNext()) {
                int id = cursor.getInt(0);
                String nombre = cursor.getString(1);
                String email = cursor.getString(2);
                tempUsuarios.add(new Usuario(id, nombre, email));
                tempLista.add(nombre + " - " + email);
            }
            cursor.close();
            db.close();

            handler.post(() -> {
                this.usuarios = tempUsuarios;
                this.lista = tempLista;
                adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, lista);
                listUsuarios.setAdapter(adapter);
            });
        });
    }

    private void mostrarDialogo(Usuario usuario) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Editar o Eliminar");

        final EditText inputNombre = new EditText(this);
        inputNombre.setText(usuario.nombre);
        final EditText inputEmail = new EditText(this);
        inputEmail.setText(usuario.getEmail());

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(inputNombre);
        layout.addView(inputEmail);
        builder.setView(layout);

        builder.setPositiveButton("Actualizar", (dialog, which) -> {
            actualizarUsuario(usuario.id, inputNombre.getText().toString(), inputEmail.getText().toString());
        });

        builder.setNegativeButton("Eliminar", (dialog, which) -> {
            eliminarUsuario(usuario.id);
        });

        builder.setNeutralButton("Cancelar", null);
        builder.show();
    }

    private void actualizarUsuario(int id, String nombre, String email) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("nombre", nombre);
            values.put("email", email);
            db.update("usuarios", values, "id=?", new String[]{String.valueOf(id)});
            db.close();
            runOnUiThread(this::cargarUsuarios);
        });
    }

    private void eliminarUsuario(int id) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("usuarios", "id=?", new String[]{String.valueOf(id)});
            db.close();
            runOnUiThread(this::cargarUsuarios);
        });
    }
}
