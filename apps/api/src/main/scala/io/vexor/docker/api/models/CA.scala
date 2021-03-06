package io.vexor.docker.api.models

class CA(val id: String, val subject: String, val re: KeyGen.Result, pem: KeyGen.Pem) {
  val cert       = re.cert
  val privateKey = re.privateKey
  val publicKey  = re.publicKey

  val certPem    = pem.cert
  val keyPem     = pem.privateKey
}

object CA {
  val issuer = "vexor.io"

  def apply(id: String, subject: String, prop: PropertiesTable): CA = {
    val certName = s"$id.cacert"
    val keyName  = s"$id.cakey"

    val ca  = loadFromDb(prop, certName, keyName) getOrElse genAndSaveToDb(prop, subject, certName, keyName)
    val pem = KeyGen.toPEM(ca)
    new CA(id, subject, ca, pem)
  }

  private def loadFromDb(prop: PropertiesTable, certName: String, keyName: String) : Option[KeyGen.Result] = {
    val pem =
      for {
        cert <- prop.one(certName)
        key  <- prop.one(keyName)
      } yield KeyGen.Pem(cert.value, key.value)

    pem map KeyGen.fromPEM
  }

  private def genAndSaveToDb(prop: PropertiesTable, subject: String, certName: String, keyName: String): KeyGen.Result = {
    val ca  = KeyGen.genCa(issuer, subject)
    val pem = KeyGen.toPEM(ca)

    prop.save(PropertiesTable.Record(certName, pem.cert))
    prop.save(PropertiesTable.Record(keyName,  pem.privateKey))

    ca
  }
}
