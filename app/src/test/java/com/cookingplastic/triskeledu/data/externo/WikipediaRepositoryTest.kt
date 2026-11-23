package com.cookingplastic.triskeledu.data.externo

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

// La API de Wikipedia se simula con MockK: la prueba no usa internet y revisa solo la lógica del repositorio
class WikipediaRepositoryTest {

    private val api = mockk<WikipediaApi>()
    private val repositorio = RetrofitWikipediaRepository(api)

    private val urlMoneda = "https://es.wikipedia.org/api/rest_v1/page/summary/Palacio_de_La_Moneda"

    @Test
    fun enlaceDeArticulo_seConvierteEnLaUrlDelResumen() {
        assertEquals(urlMoneda, EnlaceWikipedia.urlResumen("https://es.wikipedia.org/wiki/Palacio_de_La_Moneda"))
    }

    @Test
    fun enlaceMovilOConAncla_conservaElTituloYElIdioma() {
        assertEquals(
            "https://en.wikipedia.org/api/rest_v1/page/summary/Santiago",
            EnlaceWikipedia.urlResumen("https://en.m.wikipedia.org/wiki/Santiago#Historia")
        )
    }

    @Test
    fun enlaceQueNoEsDeWikipedia_oVacio_daNull() {
        assertNull(EnlaceWikipedia.urlResumen("https://www.monumentos.gob.cl/monumentos"))
        assertNull(EnlaceWikipedia.urlResumen(""))
        assertNull(EnlaceWikipedia.urlResumen(null))
    }

    @Test
    fun resumen_devuelveTituloExtractoEImagen() = runTest {
        coEvery { api.resumen(urlMoneda) } returns ResumenWikipediaDto(
            titulo = "Palacio de La Moneda",
            extracto = "  Sede del presidente de Chile.  ",
            miniatura = ImagenDto("https://upload.wikimedia.org/moneda.jpg"),
            tipo = "standard"
        )

        val r = repositorio.resumen("https://es.wikipedia.org/wiki/Palacio_de_La_Moneda")

        assertEquals(ResumenLugar("Palacio de La Moneda", "Sede del presidente de Chile.", "https://upload.wikimedia.org/moneda.jpg"), r)
    }

    @Test
    fun paginaDeDesambiguacion_oSinExtracto_daNull() = runTest {
        coEvery { api.resumen(any()) } returns ResumenWikipediaDto("Santiago", "Puede referirse a...", null, "disambiguation")
        assertNull(repositorio.resumen("https://es.wikipedia.org/wiki/Santiago"))

        coEvery { api.resumen(any()) } returns ResumenWikipediaDto("Vacío", " ", null, "standard")
        assertNull(repositorio.resumen("https://es.wikipedia.org/wiki/Vacio"))
    }

    @Test
    fun enlaceAjeno_noLlamaALaApi() = runTest {
        assertNull(repositorio.resumen("https://www.google.com"))
        coVerify(exactly = 0) { api.resumen(any()) }
    }

    @Test(expected = IOException::class)
    fun sinRed_laExcepcionLlegaAlViewModel() = runTest {
        coEvery { api.resumen(any()) } throws IOException("sin red")
        repositorio.resumen("https://es.wikipedia.org/wiki/Palacio_de_La_Moneda")
    }
}
