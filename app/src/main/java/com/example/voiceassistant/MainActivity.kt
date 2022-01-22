package com.example.voiceassistant

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.SimpleAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    val TAG: String = "MainActivity" //Установка переменной для идентификации логов

    lateinit var requestInput: TextInputEditText //Переменная для поля ввода текста

    lateinit var podsAdapter: SimpleAdapter //Адаптер для обработки данных в ListView

    lateinit var progressBar: ProgressBar //Переменная для прогресс-бара

    val pods = mutableListOf<HashMap<String, String>>( //Изменяемый список со словарями типа "Ключ-значение" (текстового типа)
        HashMap<String, String>().apply {
            put("Title","Title 1") //Добавление элементов в словарь
            put("Content", "Content 1")
        },
        HashMap<String, String>().apply {
            put("Title","Title 2") //Добавление элементов в словарь
            put("Content", "Content 2")
        },
        HashMap<String, String>().apply {
            put("Title","Title 3") //Добавление элементов в словарь
            put("Content", "Content 3")
        },
        HashMap<String, String>().apply {
            put("Title","Title 4") //Добавление элементов в словарь
            put("Content", "Content 4")
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initView() //Вызов метода инициализации
    }

    //Инициализация для объектов программы
    fun initView(){
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar) //Включение поддержки меню

        requestInput = findViewById(R.id.text_input_edit) //Текстовый запрос

        val podsList: ListView = findViewById(R.id.pods_list) //Список резульататов поиска

        podsAdapter = SimpleAdapter(
            applicationContext, //Указывает внутри какого приложения устанавливается адаптер
            pods, //Список с объектами
            R.layout.item_pod, //Какую разметку использовать для элемента
            arrayOf("Title", "Content"), //Связывает ключи данных, которые будут передавать, с объектами внутри элементов списка
            intArrayOf(R.id.title, R.id.content)
        )
        podsList.adapter = podsAdapter

        val voiceInputButton: FloatingActionButton = findViewById(R.id.voice_input_button)
        voiceInputButton.setOnClickListener{
            Log.d(TAG, "FAB")
        }

        progressBar = findViewById(R.id.progress_bar)
    }

    //Создание кнопок в меню
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu) //по этому методу буду читать доп. инфу
        return super.onCreateOptionsMenu(menu)
    }

    //Функция, срабатывающее при клике на пункт меню
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) { //Конструкция, похожая семантикой на if-else
            R.id.action_stop -> { //Когда выбрана кнопка "Стоп"
                Log.d(TAG, "action_stop") //Логируем нажатие кнопки "Стоп"
                return true
            }
        }
        when(item.itemId) {
            R.id.action_clear -> { //Когда выбрана кнопка "Очистить"
                Log.d(TAG, "action_clear") //Логируем нажатие кнопки "Очистить"
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}