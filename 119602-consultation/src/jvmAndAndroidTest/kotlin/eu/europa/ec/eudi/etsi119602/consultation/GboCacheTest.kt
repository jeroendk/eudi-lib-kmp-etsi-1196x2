@file:OptIn(eu.europa.ec.eudi.etsi1196x2.consultation.SensitiveApi::class)

package eu.europa.ec.eudi.etsi119602.consultation

import eu.europa.ec.eudi.etsi119602.datamodel.Uri
import eu.europa.ec.eudi.etsi1196x2.consultation.SupportedLists
import eu.europa.ec.eudi.etsi1196x2.consultation.VerificationContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.io.files.Path
import java.nio.file.Files
import kotlin.test.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class GboCacheTest {
    @Test fun hourlyRefreshAndVerifiedOutageCache() = runTest {
        val directory = Files.createTempDirectory("gbo-lote-cache")
        val uri = Uri("https://trust.example/lote/PIDProviders.jwt")
        var now = Instant.parse("2026-11-11T00:00:00Z")
        val clock = object : Clock { override fun now() = now }
        val store = LoTEFileStore(Path(directory.toString()))
        val jwt = checkNotNull(javaClass.getResourceAsStream("/gbo-lote/valid.jwt")).bufferedReader().use { it.readText() }
        val initial = now
        var calls = 0
        var status = HttpStatusCode.OK
        var content = jwt
        val http = HttpClient(MockEngine { calls++; respond(content, status) })
        val verifier = VerifyJwtSignature { raw ->
            if (raw == jwt) VerifyJwtSignature.Outcome.Verified(raw)
            else VerifyJwtSignature.Outcome.NotVerified(IllegalArgumentException("invalid signature"))
        }
        val cache = LoadSingleLoTEWithFileCache(Path(directory.toString()), downloadSingleLoTE = DownloadSingleLoTE(http), clock = clock, fileCacheExpiration = 1.hours, refreshInterval = 1.hours)
        val loader = LoadLoTEAndPointers(LoadLoTEAndPointers.Constraints.DoNotLoadOtherPointers, verifier, cache, clock = clock)
        suspend fun verified() {
            assertTrue(loader(uri).toList().single() is LoadLoTEAndPointers.Event.LoTELoaded)
        }
        try {
            verified()
            assertEquals(jwt, store.read(uri)?.jwt)
            assertEquals(1, calls)
            now = initial + 1.hours - kotlin.time.Duration.parse("1s")
            verified()
            assertEquals(1, calls)
            now = initial + 1.hours
            verified()
            assertEquals(2, calls)
            now += 1.hours
            content = "malicious replacement"
            verified()
            assertEquals(jwt, store.read(uri)?.jwt)
            status = HttpStatusCode.ServiceUnavailable
            now += 1.hours
            verified()
            now = initial + 31.days
            assertTrue(loader(uri).toList().none { it is LoadLoTEAndPointers.Event.LoTELoaded }) // Expired cache never yields anchors.
        } finally { http.close(); directory.toFile().deleteRecursively() }
    }

    @Test fun gboRolesAreSeparateAndPubEaaIsAvailable() {
        val meta = SupportedLists.gboDemo()
        assertEquals(setOf(VerificationContext.PID, VerificationContext.PIDStatus), meta.pidProviders!!.svcTypePerCtx.keys)
        assertEquals(setOf(VerificationContext.PubEAA, VerificationContext.PubEAAStatus), meta.pubEaaProviders!!.svcTypePerCtx.keys)
        assertEquals(setOf(VerificationContext.WalletRelyingPartyAccessCertificate), meta.wrpacProviders!!.svcTypePerCtx.keys)
        assertEquals(setOf(VerificationContext.WalletRelyingPartyRegistrationCertificate, VerificationContext.WalletRelyingPartyRegistrationCertificateStatus), meta.wrprcProviders!!.svcTypePerCtx.keys)
        assertNull(meta.walletProviders)
        assertNull(meta.qeaProviders)
    }
}
