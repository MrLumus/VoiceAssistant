package com.example.voiceassistant

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.SimpleAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.wolfram.alpha.WAEngine
import com.wolfram.alpha.WAPlainText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.StringBuilder

class MainActivity : AppCompatActivity() {

    val TAG: String = "MainActivity" //Установка переменной для идентификации логов

    lateinit var requestInput: TextInputEditText //Переменная для поля ввода текста

    lateinit var podsAdapter: SimpleAdapter //Адаптер для обработки данных в ListView

    lateinit var progressBar: ProgressBar //Переменная для прогресс-бара

    lateinit var waEngine: WAEngine //Создание объекта Wolfram Engine

    val pods = mutableListOf<HashMap<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initView() //Вызов метода инициализации View
        initWolframEngine() //Вызов метода инизиализации Wolfram Engine
    }

    //Инициализация для объектов программы
    fun initView(){
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar) //Включение поддержки меню

        requestInput = findViewById(R.id.text_input_edit) //Текстовый запрос
        requestInput.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE){
                pods.clear()
                podsAdapter.notifyDataSetChanged()

                val question = requestInput.text.toString()
                askWolfram(question)
            }

            return@setOnEditorActionListener false
        }

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
                requestInput.text?.clear()
                pods.clear()
                podsAdapter.notifyDataSetChanged()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun initWolframEngine(){ //Инициализцаия Wolfram Alpha
        waEngine = WAEngine().apply {
            appID = "3QH559-HW8E8UWK7Q" //Передаем ключ
            addFormat("plaintext") //Указываем формат "плоский" текст
        }
    }

    fun showSnackbar(message: String){ //Метод вызова снэкбара
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_INDEFINITE).apply { //Указываем, что находится в элементе Content, выводит message, длится бесконечно
            setAction(android.R.string.ok) { //Добавляем действие на кнопку OK
                dismiss() //Закрытие снэкбара
            }
            show() //Отображение снэкбара
        }
    }

    fun askWolfram(request: String){ //Запрос к Wolfram Alpha
        progressBar.visibility = View.VISIBLE //Отображаем прогрессбар
        CoroutineScope(Dispatchers.IO).launch { //Запускаем второстепенный поток IO
            val query = waEngine.createQuery().apply { input = request } //Создаем запрос со входным значением request
            runCatching {
                waEngine.performQuery(query) //Отправляем запрос
            }.onSuccess { result ->
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE //Скрываем прогрессбар
                    if (result.isError){ //Если все-таки ошибка
                        showSnackbar(result.errorMessage) //Выводим сообщение ошибки
                        return@withContext //ЗАканчиваем выполнение функции
                    }
                    if (!result.isSuccess){ //Если не успех
                        requestInput.error = getString(R.string.error_do_not_understand) //Возвращаем сообщение "ЧТо-то пошло не так"
                        return@withContext //Заканчиваем выполнение функции
                    }

                    for (pod in result.pods){ //Перебираем поды
                        if (pod.isError) continue //Если ошибка в поде перескакиваем на следующую итерацию
                        val content = StringBuilder() //Инициализируем переменную для контента

                        for(subpod in pod.subpods){ //Перебираем сабподы в подах
                            for(element in subpod.contents){ //Перебираем каждый элемент в сабподе
                                if (element is WAPlainText){ //Если элемент является текстом
                                    content.append(element.text) //Добавляем к контенту текстовое содержимое элемента
                                }
                            }
                        }
                        pods.add(0, HashMap<String, String>().apply { //Добавляем новый результат в список подов
                            put("Title", pod.title) //Вставляем загаловок
                            put("Content", content.toString()) //Вставляем контент
                        })
                    }

                    podsAdapter.notifyDataSetChanged() //Пересобираем адаптер
                }
            }.onFailure { t -> //Если ошибка
                withContext(Dispatchers.Main) { //Переключаемся на основной поток
                    progressBar.visibility = View.GONE //Убираем прогрессбар
                    showSnackbar(t.message ?: getString(R.string.error_something_went_wrong)) //Выводим сообщение об ошибке или нашу ошибку
                }
            }
        }
    }
}