package com.example.consultadivisas

import android.app.Application
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import java.util.TimeZone

//Representa un registro de tipo de cambio con su timestamp y su valor
data class ExchangeEntry(val timestamp: Float, val rate: Float)

class ExchangeRateViewModel(application: Application) : AndroidViewModel(application) {

    //Preferencias compartidas para almacenar la configuracion de la app
    private val prefs = application.getSharedPreferences("exchange_data", Context.MODE_PRIVATE)
    private val gson = Gson()
    //Claves para almacenar datos en las preferencias
    private val cacheKey = "entries"
    private val currencyKey = "selected_currency"
    private val startDateKey = "start_date_millis"
    private val endDateKey = "end_date_millis"

    //Flujo de datos en tiempo real de los registros de tipo de cambio
    private val _entries = MutableStateFlow<List<ExchangeEntry>>(emptyList())
    val entries: StateFlow<List<ExchangeEntry>> = _entries

    //Fechas persistentes en milisegundos (por defecto los 7 utlimos dias)
    var startDateMillis: Long = prefs.getLong(startDateKey, System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L)
    var endDateMillis: Long = prefs.getLong(endDateKey, System.currentTimeMillis())

    //Obtiene la moneda guardada en preferencias, por defecto "trumpcoins"
    fun getPersistedCurrency(): String {
        return prefs.getString(currencyKey, "USD") ?: "USD"
    }
    //Guarda la moneda seleccionada en preferencias
    fun persistCurrency(currency: String) {
        prefs.edit().putString(currencyKey, currency).apply()
    }
    //Obtiene la fecha de inicio guardada en preferencias
    fun getPersistedStartDate(): Long {
        return prefs.getLong(startDateKey, startDateMillis)
    }
    //Obtiene la fecha de fin guardada en preferencias
    fun getPersistedEndDate(): Long {
        return prefs.getLong(endDateKey, endDateMillis)
    }
    //Guarda las fechas seleccionadas en preferencias
    fun persistDates(start: Long, end: Long) {
        prefs.edit().putLong(startDateKey, start).putLong(endDateKey, end).apply()
    }

    //Carga los datos almacenados en caché al iniciar el ViewModel
    init {
        loadCachedData()
    }

    //Realiza una consulta al ContentProvider para obtener datos de tipo de cambio filtrados por
    // moneda y fechas
    fun loadDataFromContentProvider(
        context: Context,
        currency: String,
        startDateMillis: Long,
        endDateMillis: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            //Define el formato de fecha esperado por el ContentProvider
            val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")

            //Convierte las fechas a String en el formato requerido
            val startDateStr = dateFormat.format(Date(startDateMillis))
            val endDateStr = dateFormat.format(Date(endDateMillis))

            //Se construye la URI con los parametros de consulta
            val uri = Uri.parse("content://com.example.divisawapi.divisasprovider/exchange_rates")
                .buildUpon()
                .appendQueryParameter("currency", currency)
                .appendQueryParameter("startDate", startDateStr)
                .appendQueryParameter("endDate", endDateStr)
                .build()

            val tempList = mutableListOf<ExchangeEntry>()

            //Consulta al ContentProvider
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use { c ->
                while (c.moveToNext()) {
                    val timeStr = c.getString(c.getColumnIndexOrThrow("timeLastUpdateUtc"))
                    val rate = c.getDouble(c.getColumnIndexOrThrow("exchangeRate"))
                    try {
                        //Parsear la fecha en UTC
                        val dateUtc = dateFormat.parse(timeStr)
                        if (dateUtc != null) {
                            //Convertir el timestamp UTC a hora local
                            val offset = TimeZone.getDefault().getOffset(dateUtc.time).toLong()
                            val localMillis = dateUtc.time + offset
                            val entry = ExchangeEntry(localMillis.toFloat(), rate.toFloat())
                            tempList.add(entry)

                            //Depurar que registros van llegando
                            Log.e("ExchangeRateViewModel", "Registro: id=${c.getInt(c.getColumnIndexOrThrow("id"))}, fechaLocal=${Date(localMillis)}, rate=$rate")

                        }
                    } catch (e: Exception) {
                        Log.e("ExchangeRateViewModel", "Error parseando fecha: ${e.localizedMessage}")
                    }
                }
            }

            //Ordenar los registros por timestamp y actualizar el estado
            tempList.sortBy { it.timestamp }
            withContext(Dispatchers.Main) {
                Log.d("ExchangeRateViewModel", "Se han cargado ${tempList.size} registros")
                updateEntries(tempList)
            }
        }
    }


    //Carga los datos almacenados en SharedPreferences y los asigna al estado si estan disponibles
    //Se ejecuta al inicializar el ViewModel para recuperar datos previos
    private fun loadCachedData() {
        val json = prefs.getString(cacheKey, null) //Obtiene los datos de SharedPreferences
        if (json != null) { //Verifica si hay datos almacenados
            val type = object : TypeToken<List<ExchangeEntry>>() {}.type //Define el tipo de la lista
            val list: List<ExchangeEntry>? = gson.fromJson(json, type) //Convierte el JSON en una lista de objetos ExchangeEntry
            if (list != null) { //Si la conversin es exitosa, asigna la lista al estado.
                _entries.value = list
            }
        }
    }

    //Guarda los datos en SharedPreferences para conservar los registros cargados
    //Esto permite que los datos se mantengan disponibles incluso si la aplicación se cierra y
    //se vuelve a abrir
    private fun cacheData(data: List<ExchangeEntry>) {
        val json = gson.toJson(data) //Convierte la lista en formato JSON
        prefs.edit().putString(cacheKey, json).apply() //Guarda el JSON en SharedPreferences
    }


    //Esta función se llama cuando se obtienen nuevos datos desde el ContentProvider
    //Primero actualiza el flujo de datos _entries con los nuevos registros y luego los almacena en
    //SharedPreferences para su persistencia
    fun updateEntries(newEntries: List<ExchangeEntry>) {
        _entries.value = newEntries //Actualiza la lista de datos
        cacheData(newEntries) //Guarda los nuevos datos
    }
}
