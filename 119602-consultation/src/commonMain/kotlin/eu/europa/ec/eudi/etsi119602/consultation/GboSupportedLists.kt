// GBO reference demo profile. Reuses ETSI service identifiers, without claiming
// the EU list/certificate profile. Scheme and signer are checked independently
// by the wallet's GBO JWT verifier. Only CA anchors are accepted.
package eu.europa.ec.eudi.etsi119602.consultation

import eu.europa.ec.eudi.etsi119602.consultation.eu.ServiceDigitalIdentityCertificateType
import eu.europa.ec.eudi.etsi119602.datamodel.Uri
import eu.europa.ec.eudi.etsi1196x2.consultation.SupportedLists
import eu.europa.ec.eudi.etsi1196x2.consultation.VerificationContext

public fun SupportedLists.Companion.gboDemo(): SupportedLists<LotEMeta<VerificationContext>> {
    fun role(name: String, issuance: VerificationContext, status: VerificationContext? = null): LotEMeta<VerificationContext> = LotEMeta(
        svcTypePerCtx = buildMap {
            put(issuance, LotEMeta.SvcAndEEProfile(Uri("http://uri.etsi.org/19602/SvcType/$name/Issuance"), null))
            status?.let { put(it, LotEMeta.SvcAndEEProfile(Uri("http://uri.etsi.org/19602/SvcType/$name/Revocation"), null)) }
        },
        serviceDigitalIdentityCertificateType = ServiceDigitalIdentityCertificateType.CA,
    )
    return SupportedLists(
        pidProviders = role("PID", VerificationContext.PID, VerificationContext.PIDStatus),
        pubEaaProviders = role("PubEAA", VerificationContext.PubEAA, VerificationContext.PubEAAStatus),
        wrpacProviders = role("WRPAC", VerificationContext.WalletRelyingPartyAccessCertificate),
        wrprcProviders = role("WRPRC", VerificationContext.WalletRelyingPartyRegistrationCertificate, VerificationContext.WalletRelyingPartyRegistrationCertificateStatus),
    )
}
