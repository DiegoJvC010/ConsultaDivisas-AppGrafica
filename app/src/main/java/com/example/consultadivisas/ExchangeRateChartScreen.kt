package com.example.consultadivisas

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExchangeRateChartScreen(viewModel: ExchangeRateViewModel = viewModel()) {
    val context = LocalContext.current

    //Observa los datos almacenados en el ViewModel
    val cachedEntries by viewModel.entries.collectAsState()

    //Estado para la moneda seleccionada, se mantiene guardada
    var selectedCurrency by rememberSaveable { mutableStateOf(viewModel.getPersistedCurrency()) }
    var expanded by remember { mutableStateOf(false) }

    //Estados para las fechas seleccionadas (almacenadas en milisegundos)
    var startDateMillis by rememberSaveable { mutableStateOf(viewModel.getPersistedStartDate()) }
    var endDateMillis by rememberSaveable { mutableStateOf(viewModel.getPersistedEndDate()) }
    var showValues by rememberSaveable { mutableStateOf(false) }

    //Al iniciar la pantalla, se carga automáticamente la última selección de datos
    LaunchedEffect(Unit) {
        val startUtc = startDateMillis - TimeZone.getDefault().getOffset(startDateMillis)
        val endUtc = endDateMillis - TimeZone.getDefault().getOffset(endDateMillis)

        viewModel.loadDataFromContentProvider(
            context = context,
            currency = selectedCurrency,
            startDateMillis = startUtc,
            endDateMillis = endUtc
        )

        Log.d("jijiijijjjii", "si carga chaval")
    }

    //Lista de divisas disponibles para seleccion
    val currencyList = listOf(
        "AED", "AFN", "ALL", "AMD", "ANG", "AOA", "ARS", "AUD", "AWG",
        "AZN", "BAM", "BBD", "BDT", "BGN", "BHD", "BIF", "BMD", "BND", "BOB",
        "BRL", "BSD", "BTN", "BWP", "BYN", "BZD", "CAD", "CDF", "CHF", "CLP",
        "CNY", "COP", "CRC", "CUP", "CVE", "CZK", "DJF", "DKK", "DOP", "DZD",
        "EGP", "ERN", "ETB", "EUR", "FJD", "FKP", "FOK", "GBP", "GEL", "GGP",
        "GHS", "GIP", "GMD", "GNF", "GTQ", "GYD", "HKD", "HNL", "HRK", "HTG",
        "HUF", "IDR", "ILS", "IMP", "INR", "IQD", "IRR", "ISK", "JEP", "JMD",
        "JOD", "JPY", "KES", "KGS", "KHR", "KID", "KMF", "KRW", "KWD", "KYD",
        "KZT", "LAK", "LBP", "LKR", "LRD", "LSL", "LYD", "MAD", "MDL", "MGA",
        "MKD", "MMK", "MNT", "MOP", "MRU", "MUR", "MVR", "MWK", "MYR", "MZN",
        "NAD", "NGN", "NIO", "NOK", "NPR", "NZD", "OMR", "PAB", "PEN", "PGK",
        "PHP", "PKR", "PLN", "PYG", "QAR", "RON", "RSD", "RUB", "RWF", "SAR",
        "SBD", "SCR", "SDG", "SEK", "SGD", "SHP", "SLE", "SLL", "SOS", "SRD",
        "SSP", "STN", "SYP", "SZL", "THB", "TJS", "TMT", "TND", "TOP", "TRY",
        "TTD", "TVD", "TWD", "TZS", "UAH", "UGX", "USD", "UYU", "UZS", "VES",
        "VND", "VUV", "WST", "XAF", "XCD", "XDR", "XOF", "XPF", "YER", "ZAR",
        "ZMW", "ZWL"
    )

    //Formato de fecha para mostrar en la UI
    val displayDateFormat = SimpleDateFormat("EEEE, dd MMM yyyy HH:mm",
        Locale("es", "MX"))
    fun formatDisplayDate(millis: Long): String = displayDateFormat.format(Date(millis))

    //Funcion para convertir la hora local a UTC
    fun localToUtc(localMillis: Long): Long {
        val offset = TimeZone.getDefault().getOffset(localMillis).toLong()
        return localMillis - offset
    }

    //Funcion para mostrar un dialogo de seleccion de fecha y hora
    fun showDateTimePicker(initialMillis: Long, onDateTimeSelected: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = initialMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                // Actualizamos la fecha
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                // Una vez seleccionado la fecha, mostramos el TimePicker
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        cal.set(Calendar.MINUTE, minute)
                        onDateTimeSelected(cal.timeInMillis)
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true // 24 horas
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    //UI de la aplicacion
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Consulta de Divisas") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //Card que agrupa los controles de seleccion
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    //Menú desplegable para la moneda
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = "Moneda: $selectedCurrency",
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            currencyList.forEach { currency ->
                                DropdownMenuItem(
                                    text = { Text(currency) },
                                    onClick = {
                                        selectedCurrency = currency
                                        viewModel.persistCurrency(currency)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    //Selección de rango de fechas
                    Column(modifier = Modifier.fillMaxWidth()) {
                        //Botón "Desde"
                        Button(
                            onClick = { showDateTimePicker(startDateMillis) { startDateMillis = it } },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = "Desde:\n${formatDisplayDate(startDateMillis)}",
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        //Botón "Hasta"
                        Button(
                            onClick = { showDateTimePicker(endDateMillis) { endDateMillis = it } },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = "Hasta:\n${formatDisplayDate(endDateMillis)}",
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    //Botón para cargar datos (se reordena el rango si es necesario)
                    Button(
                        onClick = {
                            if (startDateMillis > endDateMillis) {
                                //Intercambiamos si el orden es invertido
                                val temp = startDateMillis
                                startDateMillis = endDateMillis
                                endDateMillis = temp
                            }
                            viewModel.persistDates(startDateMillis, endDateMillis)
                            val startUtc = localToUtc(startDateMillis)
                            val endUtc = localToUtc(endDateMillis)
                            viewModel.loadDataFromContentProvider(
                                context = context,
                                currency = selectedCurrency,
                                startDateMillis = startUtc,
                                endDateMillis = endUtc
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Cargar Datos", color = MaterialTheme.colorScheme.onSecondary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            //Convierte los datos almacenados en `cachedEntries` en una lista de puntos para el gráfico
            val chartEntries = cachedEntries.map { Entry(it.timestamp, it.rate) }

            //Si no hay datos disponibles, se muestra un mensaje en pantalla
            if (chartEntries.isEmpty()) {
                Text(
                    text = "No hay datos para mostrar chavo", //Mensaje cuando no hay datos
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            } else {
                //Si hay datos se genera el gráfico de líneas
                AndroidView(
                    //factory crea el gráfico dentro de la UI de Android
                    factory = { ctx ->
                        LineChart(ctx).apply {
                            setNoDataText("Sin datos") //Mensaje cuando no hay datos en el gráfico
                            description.isEnabled = false //Se desactiva la descripción del gráfico
                            setBackgroundColor(Color.WHITE) //Fondo del gráfico

                            //Configuración de los ejes
                            xAxis.textColor = Color.BLACK //Color de los textos en el eje X
                            axisLeft.textColor = Color.BLACK //Color del texto en el eje Y izquierdo
                            axisRight.isEnabled = false //Se oculta el eje derecho del gráfico
                            legend.textColor = Color.BLACK //Color del texto en la leyenda
                        }
                    },
                    //update actualiza los datos y la configuración del gráfico
                    update = { chart ->
                        //Se crea un conjunto de datos (LineDataSet) para el gráfico
                        val dataSet = LineDataSet(chartEntries,
                            "Tipo de cambio 1 MXN a $selectedCurrency").apply {
                            lineWidth = 3f //Grosor de la línea
                            setDrawCircles(true) //Habilita los puntos en la línea
                            setCircleRadius(4f) //Aumenta el tamaño de las bolitas
                            setCircleHoleRadius(3f)//Ajusta el hueco del centro
                            setDrawValues(showValues) //Para mostrar los valores sobre los puntos
                            color = Color.CYAN //Color de la línea
                            setCircleColor(Color.CYAN) //Color de los puntos
                            setValueTextSize(7f) //Tamaño de los valores
                        }

                        //Asigna los datos al gráfico
                        chart.data = LineData(dataSet)

                        //Formateador para el eje X (convierte timestamps a fechas legibles)
                        chart.xAxis.valueFormatter = object : ValueFormatter() {
                            private val sdf = SimpleDateFormat("dd MMM HH:mm", Locale("es", "MX"))
                            override fun getFormattedValue(value: Float): String {
                                return sdf.format(Date(value.toLong())) //Convierte el timestamp a fecha legible
                            }
                        }

                        //Inclina las etiquetas del eje X para mejor visibilidad
                        chart.xAxis.labelRotationAngle = -45f

                        //Notifica al gráfico que los datos han cambiado y lo redibuja
                        chart.notifyDataSetChanged()
                        chart.invalidate()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            //Botón para mostrar u ocultar valores en el gráfico
            Button(
                onClick = { showValues = !showValues },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text(if (showValues) "Ocultar valores" else "Mostrar valores",
                    color = MaterialTheme.colorScheme.onTertiary)
            }
        }
    }
}
