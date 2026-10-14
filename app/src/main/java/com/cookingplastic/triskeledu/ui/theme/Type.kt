package com.cookingplastic.triskeledu.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cookingplastic.triskeledu.R

// Fuentes en app/src/main/res/font/ (Google Fonts, licencia SIL Open Font License 1.1).
val Baloo2 = FontFamily(
    Font(R.font.baloo2_bold, FontWeight.Bold),
    Font(R.font.baloo2_extrabold, FontWeight.ExtraBold),
)

val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
)

// Escala del sistema visual: Baloo 2 para títulos y cifras, Nunito para todo lo demás.
val Typography = Typography(
    // Cifras grandes: "45" en Mis lugares
    displaySmall = TextStyle(fontFamily = Baloo2, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 38.sp),

    // Títulos de pantalla: "Aprendo IA", "Configuración", "La Moneda" en el detalle
    headlineLarge = TextStyle(fontFamily = Baloo2, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = Baloo2, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = Baloo2, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 30.sp),

    // Títulos de tarjeta y logros: "La Moneda" en el mapa, "+5 puntos"
    titleLarge = TextStyle(fontFamily = Baloo2, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 26.sp),
    // Nombres en listas y actividades: "Mira el video sobre IA"
    titleMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, lineHeight = 22.sp),
    // Encabezados de sección: "Descripción", "Actividades"
    titleSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, lineHeight = 21.sp),

    // Texto corrido
    bodyLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),

    // labelLarge es el estilo que Material 3 usa por defecto en los botones
    labelLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, lineHeight = 22.sp),
    // Etiquetas de campos y chips: "Correo", "Monumento"
    labelMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp),
    // Barra de navegación y textos mínimos: "a 120 m · Completado"
    labelSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 17.sp),
)