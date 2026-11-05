package com.cookingplastic.triskeledu.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.cookingplastic.triskeledu.data.model.TemaApp

/*
* Los 8 temas de la app (RN-32), cada uno en versión Día y Noche: 16 paletas.
* Cada paleta tiene los 16 colores de la tabla "Paletas base" del ERS v2.7.
* La base de cada tema viene del ERS; la contraparte (oscura para los claros y clara para los oscuros)
* mantiene el tono de Primario y Puntos y cumple contraste 4,5:1 (lo revisa ContrasteTemasTest).
*/
@Immutable
data class PaletaTema(
    val primario: Color,
    val sobrePrimario: Color,
    val primarioContenedor: Color,
    val sobreContenedor: Color,
    //relleno de la esfera de avance
    val agua: Color,
    //chip de puntos y marcador desbloqueado
    val puntos: Color,
    val sobrePuntos: Color,
    val puntosClaro: Color,
    val fondo: Color,
    val superficie: Color,
    val borde: Color,
    val tinta: Color,
    val tintaSecundaria: Color,
    val alerta: Color,
    //marcador de un lugar sin escanear
    val bloqueado: Color,
    //punto y círculo del usuario en el mapa
    val ubicacion: Color,
    val esNoche: Boolean,
    //los siguientes salen de los 16 de arriba; el Clásico conserva los suyos de Color.kt
    val sobrePuntosClaro: Color = tinta,
    val sobreAlerta: Color = if (esNoche) Color(0xFF3A0E00) else Color(0xFFFFFFFF),
    val interruptorEncendido: Color = if (esNoche) agua else primario,
    val barraAdmin: Color = if (esNoche) primarioContenedor else sobreContenedor,
    val sobreBarraAdmin: Color = if (esNoche) tinta else fondo
) {
    //Material 3 recibe la paleta como ColorScheme; los colores de superficie se arman con los mismos tokens
    //para que tarjetas, menús y la barra inferior tengan el tono del tema
    val esquema: ColorScheme by lazy {
        val base = if (esNoche) darkColorScheme() else lightColorScheme()
        base.copy(
            primary = primario,
            onPrimary = sobrePrimario,
            primaryContainer = primarioContenedor,
            onPrimaryContainer = sobreContenedor,
            inversePrimary = if (esNoche) sobreContenedor else primarioContenedor,
            secondary = puntos,
            onSecondary = sobrePuntos,
            secondaryContainer = puntosClaro,
            onSecondaryContainer = sobrePuntosClaro,
            tertiary = agua,
            onTertiary = tinta,
            background = fondo,
            onBackground = tinta,
            surface = fondo,
            onSurface = tinta,
            surfaceVariant = superficie,
            onSurfaceVariant = tintaSecundaria,
            surfaceTint = primario,
            inverseSurface = tinta,
            inverseOnSurface = fondo,
            error = alerta,
            onError = sobreAlerta,
            outline = borde,
            outlineVariant = borde,
            surfaceBright = if (esNoche) lerp(superficie, borde, 0.5f) else fondo,
            surfaceDim = if (esNoche) fondo else lerp(superficie, borde, 0.4f),
            surfaceContainerLowest = fondo,
            surfaceContainerLow = lerp(fondo, superficie, 0.5f),
            surfaceContainer = superficie,
            surfaceContainerHigh = lerp(superficie, borde, 0.2f),
            surfaceContainerHighest = lerp(superficie, borde, 0.4f)
        )
    }
}

//CompositionLocal: cualquier pantalla lee los colores propios del tema activo (Agua, Puntos, Bloqueado,
//Ubicación…) con ColoresApp, sin escribirlos a mano (RNF-05). Parte con el Clásico de día
val LocalPaletaTema = staticCompositionLocalOf { ClasicoDia }

//la paleta que corresponde al tema elegido y al interruptor Día/Noche
fun paletaDe(tema: TemaApp, noche: Boolean): PaletaTema = when (tema) {
    TemaApp.CLASICO -> if (noche) ClasicoNoche else ClasicoDia
    TemaApp.CERRO_SANTA_LUCIA -> if (noche) CerroSantaLuciaNoche else CerroSantaLuciaDia
    TemaApp.ATARDECER_MAPOCHO -> if (noche) AtardecerMapochoNoche else AtardecerMapochoDia
    TemaApp.NOCHE_MUSEO -> if (noche) NocheMuseoNoche else NocheMuseoDia
    TemaApp.DESIERTO_FLORIDO -> if (noche) DesiertoFloridoNoche else DesiertoFloridoDia
    TemaApp.BOSQUE_NATIVO -> if (noche) BosqueNativoNoche else BosqueNativoDia
    TemaApp.GLACIAR -> if (noche) GlaciarNoche else GlaciarDia
    TemaApp.IA_NEON -> if (noche) IaNeonNoche else IaNeonDia
}

// ---------- Clásico (nivel 1): los valores de Color.kt ----------
private val ClasicoDia = PaletaTema(
    primario = Primario, sobrePrimario = Fondo, primarioContenedor = PrimarioClaro, sobreContenedor = PrimarioOscuro,
    agua = Agua, puntos = Puntos, sobrePuntos = Tinta, puntosClaro = PuntosClaro,
    fondo = Fondo, superficie = Superficie, borde = Borde, tinta = Tinta,
    tintaSecundaria = TintaSecundaria, alerta = Alerta, bloqueado = Bloqueado, ubicacion = Ubicacion,
    esNoche = false,
    sobreAlerta = Fondo
)
private val ClasicoNoche = PaletaTema(
    primario = PrimarioNoche, sobrePrimario = SobrePrimarioNoche, primarioContenedor = PrimarioContenedorNoche, sobreContenedor = SobreContenedorNoche,
    agua = AguaNoche, puntos = PuntosNoche, sobrePuntos = SobrePuntosNoche, puntosClaro = PuntosContenedorNoche,
    fondo = FondoNoche, superficie = SuperficieNoche, borde = BordeNoche, tinta = TintaNoche,
    tintaSecundaria = TintaSecundariaNoche, alerta = AlertaNoche, bloqueado = BloqueadoNoche, ubicacion = UbicacionNoche,
    esNoche = true,
    sobrePuntosClaro = SobrePuntosContenedorNoche,
    sobreAlerta = SobreAlertaNoche,
    interruptorEncendido = InterruptorNoche,
    barraAdmin = BarraAdminNoche
)

// ---------- Cerro Santa Lucía (nivel 2, base clara) ----------
private val CerroSantaLuciaDia = PaletaTema(
    primario = Color(0xFF2E7D32), sobrePrimario = Color(0xFFFFFFFF), primarioContenedor = Color(0xFFDCEFD9), sobreContenedor = Color(0xFF1B5E20),
    agua = Color(0xFF66BB6A), puntos = Color(0xFFF2B33D), sobrePuntos = Color(0xFF2A1D00), puntosClaro = Color(0xFFFDF0D2),
    fondo = Color(0xFFF6FAF4), superficie = Color(0xFFE8F2E4), borde = Color(0xFFC5DCC0), tinta = Color(0xFF132A15),
    tintaSecundaria = Color(0xFF46604A), alerta = Color(0xFFB3261E), bloqueado = Color(0xFF8C9A8D), ubicacion = Color(0xFF2563EB),
    esNoche = false
)  // base del ERS
private val CerroSantaLuciaNoche = PaletaTema(
    primario = Color(0xFF8DCD91), sobrePrimario = Color(0xFF0E250F), primarioContenedor = Color(0xFF1B4B1E), sobreContenedor = Color(0xFFCBECCC),
    agua = Color(0xFF437F46), puntos = Color(0xFFF3BC53), sobrePuntos = Color(0xFF261A03), puntosClaro = Color(0xFF453517),
    fondo = Color(0xFF101C10), superficie = Color(0xFF1C2E1D), borde = Color(0xFF375338), tinta = Color(0xFFE7F3E8),
    tintaSecundaria = Color(0xFFACC3B0), alerta = Color(0xFFFF8A80), bloqueado = Color(0xFF6E7D6F), ubicacion = Color(0xFF6EA0FF),
    esNoche = true
)  // contraparte

// ---------- Atardecer en el Mapocho (nivel 3, base clara) ----------
private val AtardecerMapochoDia = PaletaTema(
    primario = Color(0xFFB9480F), sobrePrimario = Color(0xFFFFFFFF), primarioContenedor = Color(0xFFFFE3CF), sobreContenedor = Color(0xFF7A2E06),
    agua = Color(0xFFF08A4B), puntos = Color(0xFF7C3AED), sobrePuntos = Color(0xFFFFFFFF), puntosClaro = Color(0xFFEDE4FD),
    fondo = Color(0xFFFFF8F2), superficie = Color(0xFFFBEBDD), borde = Color(0xFFEBCDB4), tinta = Color(0xFF2B1608),
    tintaSecundaria = Color(0xFF6B4A36), alerta = Color(0xFFB42318), bloqueado = Color(0xFFA08C80), ubicacion = Color(0xFF2563EB),
    esNoche = false
)  // base del ERS
private val AtardecerMapochoNoche = PaletaTema(
    primario = Color(0xFFE89A72), sobrePrimario = Color(0xFF2B1408), primarioContenedor = Color(0xFF4F2A17), sobreContenedor = Color(0xFFF4D3C2),
    agua = Color(0xFFA2511F), puntos = Color(0xFFAE86F4), sobrePuntos = Color(0xFF14052E), puntosClaro = Color(0xFF281745),
    fondo = Color(0xFF1C1410), superficie = Color(0xFF2E221C), borde = Color(0xFF534037), tinta = Color(0xFFF3ECE7),
    tintaSecundaria = Color(0xFFC6B4A9), alerta = Color(0xFFFF8A80), bloqueado = Color(0xFF867164), ubicacion = Color(0xFF6EA0FF),
    esNoche = true
)  // contraparte

// ---------- Noche de museo (nivel 4, base oscura) ----------
private val NocheMuseoDia = PaletaTema(
    primario = Color(0xFF4427A5), sobrePrimario = Color(0xFFFFFFFF), primarioContenedor = Color(0xFFE0D8F8), sobreContenedor = Color(0xFF2B1966),
    agua = Color(0xFF6C48D1), puntos = Color(0xFFF9B506), sobrePuntos = Color(0xFF271D02), puntosClaro = Color(0xFFFBF2DA),
    fondo = Color(0xFFFAFAFD), superficie = Color(0xFFEEECF6), borde = Color(0xFFCAC5DD), tinta = Color(0xFF140D26),
    tintaSecundaria = Color(0xFF493F69), alerta = Color(0xFFB3261E), bloqueado = Color(0xFF8B87A1), ubicacion = Color(0xFF2563EB),
    esNoche = false
)  // contraparte
private val NocheMuseoNoche = PaletaTema(
    primario = Color(0xFFB39DFB), sobrePrimario = Color(0xFF1E1240), primarioContenedor = Color(0xFF3B2A6E), sobreContenedor = Color(0xFFE3DAFF),
    agua = Color(0xFF7C5CD6), puntos = Color(0xFFFBBF24), sobrePuntos = Color(0xFF231600), puntosClaro = Color(0xFF3D3214),
    fondo = Color(0xFF17132A), superficie = Color(0xFF241E3D), borde = Color(0xFF3A3358), tinta = Color(0xFFEEEAF8),
    tintaSecundaria = Color(0xFFB4ACCF), alerta = Color(0xFFFF8A80), bloqueado = Color(0xFF6E6890), ubicacion = Color(0xFF7DA2FF),
    esNoche = true
)  // base del ERS

// ---------- Desierto florido (nivel 5, base clara) ----------
private val DesiertoFloridoDia = PaletaTema(
    primario = Color(0xFFB0186A), sobrePrimario = Color(0xFFFFFFFF), primarioContenedor = Color(0xFFFCE1EE), sobreContenedor = Color(0xFF6B0E40),
    agua = Color(0xFFE25AA0), puntos = Color(0xFFE9B44C), sobrePuntos = Color(0xFF2A1C00), puntosClaro = Color(0xFFFBEFD3),
    fondo = Color(0xFFFFF9F4), superficie = Color(0xFFF8ECE2), borde = Color(0xFFE8D3C3), tinta = Color(0xFF2A1420),
    tintaSecundaria = Color(0xFF6E4C5C), alerta = Color(0xFFB42318), bloqueado = Color(0xFFA3929A), ubicacion = Color(0xFF2563EB),
    esNoche = false
)  // base del ERS
private val DesiertoFloridoNoche = PaletaTema(
    primario = Color(0xFFE279B2), sobrePrimario = Color(0xFF2B081B), primarioContenedor = Color(0xFF4F1735), sobreContenedor = Color(0xFFF4C2DD),
    agua = Color(0xFF972B62), puntos = Color(0xFFEBBA5B), sobrePuntos = Color(0xFF241A04), puntosClaro = Color(0xFF453517),
    fondo = Color(0xFF1C1016), superficie = Color(0xFF2E1C26), borde = Color(0xFF533746), tinta = Color(0xFFF3E7EE),
    tintaSecundaria = Color(0xFFC5ABB7), alerta = Color(0xFFFF8A80), bloqueado = Color(0xFF7F6B75), ubicacion = Color(0xFF6EA0FF),
    esNoche = true
)  // contraparte

// ---------- Bosque nativo (nivel 6, base oscura) ----------
private val BosqueNativoDia = PaletaTema(
    primario = Color(0xFF297038), sobrePrimario = Color(0xFFFFFFFF), primarioContenedor = Color(0xFFDEF3E2), sobreContenedor = Color(0xFF1E5229),
    agua = Color(0xFF5FB97A), puntos = Color(0xFFD11018), sobrePuntos = Color(0xFFFFFFFF), puntosClaro = Color(0xFFFBDADC),
    fondo = Color(0xFFFAFCFB), superficie = Color(0xFFEDF5F1), borde = Color(0xFFC8DACF), tinta = Color(0xFF122116),
    tintaSecundaria = Color(0xFF47614F), alerta = Color(0xFFB3261E), bloqueado = Color(0xFF8B9D91), ubicacion = Color(0xFF2563EB),
    esNoche = false
)  // contraparte
private val BosqueNativoNoche = PaletaTema(
    primario = Color(0xFF8BD49A), sobrePrimario = Color(0xFF0B2513), primarioContenedor = Color(0xFF1E4A2B), sobreContenedor = Color(0xFFC8F1D0),
    agua = Color(0xFF3E8E56), puntos = Color(0xFFF2545B), sobrePuntos = Color(0xFF2B0507), puntosClaro = Color(0xFF4A1A1D),
    fondo = Color(0xFF0F1A14), superficie = Color(0xFF1A2A20), borde = Color(0xFF2F4537), tinta = Color(0xFFE7F2EA),
    tintaSecundaria = Color(0xFFA8BFAF), alerta = Color(0xFFFF8A80), bloqueado = Color(0xFF66796C), ubicacion = Color(0xFF6EA0FF),
    esNoche = true
)  // base del ERS

// ---------- Glaciar (nivel 7, base clara) ----------
private val GlaciarDia = PaletaTema(
    primario = Color(0xFF0A6E9E), sobrePrimario = Color(0xFFFFFFFF), primarioContenedor = Color(0xFFD6EEF8), sobreContenedor = Color(0xFF063F5C),
    agua = Color(0xFF4FC3DC), puntos = Color(0xFF13A8A0), sobrePuntos = Color(0xFF00201E), puntosClaro = Color(0xFFD3F3F0),
    fondo = Color(0xFFF5FAFD), superficie = Color(0xFFE6F1F7), borde = Color(0xFFC4DCE8), tinta = Color(0xFF0C2230),
    tintaSecundaria = Color(0xFF466173), alerta = Color(0xFFB3261E), bloqueado = Color(0xFF8A9BA6), ubicacion = Color(0xFF2563EB),
    esNoche = false
)  // base del ERS
private val GlaciarNoche = PaletaTema(
    primario = Color(0xFF70C3EB), sobrePrimario = Color(0xFF08202B), primarioContenedor = Color(0xFF173D4F), sobreContenedor = Color(0xFFC2E4F4),
    agua = Color(0xFF2D8295), puntos = Color(0xFF5AECE4), sobrePuntos = Color(0xFF042523), puntosClaro = Color(0xFF174542),
    fondo = Color(0xFF10181C), superficie = Color(0xFF1C282E), borde = Color(0xFF374A53), tinta = Color(0xFFE7EFF3),
    tintaSecundaria = Color(0xFFA9BAC6), alerta = Color(0xFFFF8A80), bloqueado = Color(0xFF657985), ubicacion = Color(0xFF6EA0FF),
    esNoche = true
)  // contraparte

// ---------- IA neón (nivel 8, base oscura) ----------
private val IaNeonDia = PaletaTema(
    primario = Color(0xFF0B7484), sobrePrimario = Color(0xFFFFFFFF), primarioContenedor = Color(0xFFD8F4F8), sobreContenedor = Color(0xFF0B5965),
    agua = Color(0xFF10BBD5), puntos = Color(0xFFEE2A91), sobrePuntos = Color(0xFF260315), puntosClaro = Color(0xFFFBDAEB),
    fondo = Color(0xFFF9FAFD), superficie = Color(0xFFEBEEF7), borde = Color(0xFFC3CADF), tinta = Color(0xFF0D1826),
    tintaSecundaria = Color(0xFF3F4E69), alerta = Color(0xFFB3261E), bloqueado = Color(0xFF878EA1), ubicacion = Color(0xFF2563EB),
    esNoche = false
)  // contraparte
private val IaNeonNoche = PaletaTema(
    primario = Color(0xFF22D3EE), sobrePrimario = Color(0xFF04222A), primarioContenedor = Color(0xFF0E3A44), sobreContenedor = Color(0xFFA5F3FC),
    agua = Color(0xFF0E9FB5), puntos = Color(0xFFF472B6), sobrePuntos = Color(0xFF2A0518), puntosClaro = Color(0xFF3F1630),
    fondo = Color(0xFF0B1020), superficie = Color(0xFF151C33), borde = Color(0xFF2A3556), tinta = Color(0xFFE6F1FF),
    tintaSecundaria = Color(0xFF9FB0CF), alerta = Color(0xFFFF7A7A), bloqueado = Color(0xFF5B6788), ubicacion = Color(0xFF60A5FA),
    esNoche = true
)  // base del ERS

//los temas cuya paleta base es oscura (tabla de Niveles y temas)
private val temasDeBaseOscura = setOf(TemaApp.NOCHE_MUSEO, TemaApp.BOSQUE_NATIVO, TemaApp.IA_NEON)

//la versión con que se presenta cada tema en las muestras de color: la base del ERS (el Clásico, de día)
fun paletaMuestra(tema: TemaApp): PaletaTema = paletaDe(tema, noche = tema in temasDeBaseOscura)
