package com.example.voiceassistant

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
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
import java.util.*
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {

    val TAG: String = "MainActivity" //Установка переменной для идентификации логов

    lateinit var requestInput: TextInputEditText //Переменная для поля ввода текста

    lateinit var podsAdapter: SimpleAdapter //Адаптер для обработки данных в ListView

    lateinit var progressBar: ProgressBar //Переменная для прогресс-бара

    lateinit var waEngine: WAEngine //Создание объекта Wolfram Engine

    val pods = mutableListOf<HashMap<String, String>>()

    lateinit var textToSpeach: TextToSpeech

    var isTtsReady: Boolean = false

    val VOICE_RECOGNITION_REQUEST_CODE: Int = 932

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
        iniTts()
        podsAdapter = SimpleAdapter(
            applicationContext, //Указывает внутри какого приложения устанавливается адаптер
            pods, //Список с объектами
            R.layout.item_pod, //Какую разметку использовать для элемента
            arrayOf("Title", "Content"), //Связывает ключи данных, которые будут передавать, с объектами внутри элементов списка
            intArrayOf(R.id.title, R.id.content)
        )
        podsList.adapter = podsAdapter
        podsList.setOnItemClickListener { parent, view, position, id ->
            if (isTtsReady){
                val title = pods[position]["Title"]
                val content = pods[position]["Content"]
                textToSpeach.speak(content, TextToSpeech.QUEUE_FLUSH, null, title)
            }
        }

        val voiceInputButton: FloatingActionButton = findViewById(R.id.voice_input_button)
        voiceInputButton.setOnClickListener{
            pods.clear()
            podsAdapter.notifyDataSetChanged()

            if (isTtsReady){
                textToSpeach.stop()
            }

            showVoiceInputDialog()
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
                if (isTtsReady){
                    textToSpeach.stop()
                }
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
    fun iniTts(){
        textToSpeach = TextToSpeech(this) {code ->
            if (code != TextToSpeech.SUCCESS){
                Log.e(TAG, "TTS error code: $code")
                showSnackbar(getString(R.string.error_tts_is_not_ready))
            } else {
                isTtsReady = true
            }
        }
        textToSpeach.language = Locale.US
    }

    fun showVoiceInputDialog(){
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.request_hint))
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US)
        }
        runCatching {
            startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE)
        }.onFailure { t ->
            showSnackbar(t.message ?: getString(R.string.error_voice_recognition_unavailable))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0).let { question ->
                requestInput.setText(question)
                if (question != null) {
                    askWolfram(question)
                }
            }
        }
    }

}