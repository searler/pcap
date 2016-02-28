package pcap.streams

object RequestResponseCodecs {
  import scodec.codecs._
  import scodec.bits._
  import scodec.Codec

  val boolean = uint8.xmap[Boolean](_ match {
    case 1 => true
    case 0 => false
  }, _ match {
    case true => 0
    case false => 1
  })

  val parity = uint8.xmap[Parity](_ match {
    case 2 => Even
    case 1 => Odd
    case 0 => None
  }, _ match {
    case None => 0
    case Odd => 1
    case Even => 2
  })

  def fixed[T](value: T) = constant(hex"0000").xmap[T](_ => value, _ =>
    ())

  def payload[T](codec: Codec[T]) = variableSizeBytes(uint16, codec)

  val commandRequest = constant(hex"0000")
  val booleanRequest = constant(hex"0001") ~> boolean
  val version = (uint8 :: uint8).as[Version]
  val configureError = payload(version :: ascii)

  val header = uint8 <~ ignore(8)

  val configured = payload(version.as[Configured])
  val configureRejected = configureError.as[ConfigureRejected]
  val configureFailed = configureError.as[ConfigureFailed]

  val string = payload(ascii)
  val data = payload(bytes)

  val configure = (constant(hex"0008") :: int32 :: parity :: uint8 ::
    uint8 :: boolean).as[Configure] ~
    discriminated[ConfigureResponse].by(header).typecase(10,
      configured).typecase(13, configureRejected).typecase(9,
        configureFailed)

  val writeResponse = discriminated[WriteResponse].by(header)
    .typecase(6, fixed(Written))
    .typecase(7, string.as[WriteFailed])
    .typecase(11, fixed(WriteTimeout))
    .typecase(5, fixed(WriteInProgress))

  val commandResponse = discriminated[Response].by(header)
    .typecase(15, fixed(Completed))
    .typecase(8, string.as[Failed])

  val blockingWrite = data.as[BlockingWrite] ~ writeResponse
  val nonBlockingWrite = data.as[NonBlockingWrite] ~ writeResponse

  val read = (constant(hex"0002") ~> (uint16 :: uint16).as[Read]) ~
    discriminated[ReadResponse].by(header).typecase(4,
      string.as[ReadFailed]).typecase(1, data.as[ReadData])

  val transaction: Codec[(Request, Response)] = discriminated[(Request, Response)].by(header).typecase(5, configure)
    .typecase(1, blockingWrite)
    .typecase(1, nonBlockingWrite)
    .typecase(2, fixed(FlushRead) ~ commandResponse)
    .typecase(3, fixed(FlushWrite) ~ commandResponse)
    .typecase(7, booleanRequest.as[RTS] ~ commandResponse)
    .typecase(10, booleanRequest.as[Enable] ~ commandResponse)
    .typecase(11, fixed(Close) ~ commandResponse)
    .typecase(6, read)

  sealed trait Error {
    def error: String
  }

  sealed trait Request
  case class Configure(speed: Int, parity: Parity, dataBits: Int,
    stopBits: Int, flowControl: Boolean) extends Request
  case class BlockingWrite(bytes: ByteVector) extends Request
  case class NonBlockingWrite(bytes: ByteVector) extends Request
  case object FlushRead extends Request
  case object FlushWrite extends Request
  case object Close extends Request
  case class RTS(on: Boolean) extends Request
  case class Enable(on: Boolean) extends Request
  case class Read(timeout: Int, maxLength: Int) extends Request

  sealed trait Response
  sealed trait ConfigureResponse extends Response
  case class Configured(version: Version) extends ConfigureResponse
  case class ConfigureRejected(version: Version, error: String) extends ConfigureResponse with Error
  case class ConfigureFailed(version: Version, error: String) extends ConfigureResponse with Error

  sealed trait WriteResponse extends Response
  case object Written extends WriteResponse
  case object WriteTimeout extends WriteResponse
  case object WriteInProgress extends WriteResponse
  case class WriteFailed(error: String) extends WriteResponse with Error
  case object Completed extends Response
  case class Failed(error: String) extends Response with Error

  sealed trait ReadResponse extends Response
  case class ReadFailed(error: String) extends ReadResponse with Error
  case class ReadData(bytes: ByteVector) extends ReadResponse

  case class Version(major: Int, minor: Int)

  sealed trait Parity
  case object None extends Parity
  case object Odd extends Parity
  case object Even extends Parity

}
