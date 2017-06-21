package com.gu.atom.facade

import cats.syntax.either._
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.gu.contentatom.thrift.AtomData.{Media, Profile}
import com.gu.contentatom.thrift._
import com.gu.contentatom.thrift.atom.cta.CTAAtom
import com.gu.contentatom.thrift.atom.explainer._
import com.gu.contentatom.thrift.atom.interactive.InteractiveAtom
import com.gu.contentatom.thrift.atom.guide._
import com.gu.contentatom.thrift.atom.media.{AssetType, Category, MediaAtom, Metadata, Platform, PlutoData, PrivacyStatus, Asset => MediaAsset}
import com.gu.contentatom.thrift.atom.profile._
import com.gu.contentatom.thrift.atom.qanda._
import com.gu.contentatom.thrift.atom.quiz.{Answer, QuizAtom, QuizContent, ResultBucket, ResultBuckets, ResultGroup, ResultGroups, Asset => QuizAsset, Question => QQuestion}
import com.gu.contentatom.thrift.atom.review._
import com.gu.contentatom.thrift.atom.recipe._
import com.gu.contentatom.thrift.atom.storyquestions.{QuestionSet, RelatedStoryLinkType, StoryQuestionsAtom, Question => SQuestion}
import com.gu.contentatom.thrift.atom.timeline._
import com.gu.contententity.thrift._
import com.gu.contententity.thrift.entity.restaurant.Restaurant
import com.gu.contententity.thrift.entity.game.Game
import com.gu.contententity.thrift.entity.film.Film
import com.gu.contententity.thrift.entity.person.Person
import com.gu.contententity.thrift.entity.place.Place
import com.gu.contententity.thrift.entity.organisation.Organisation
import com.gu.scanamo.DynamoFormat
import com.gu.scanamo.error._
import shapeless._
import shapeless.labelled._
import shapeless.syntax.singleton._

object AtomFacade {
  type F[K, V] = FieldType[K, V]

  /*** ENUMS ***/

  implicit val gplatform = new DynamoFormat[Platform] {
    override def read(av: AttributeValue): Either[DynamoReadError, Platform] =
      implicitly[DynamoFormat[String]].read(av).flatMap(x => Platform.valueOf(x).toRight[DynamoReadError](TypeCoercionError(new IllegalArgumentException(x + " is not a valid Platform"))))
    override def write(a: Platform): AttributeValue = implicitly[DynamoFormat[String]].write(a.name)
  }

  implicit val gassettype = new DynamoFormat[AssetType] {
    override def read(av: AttributeValue): Either[DynamoReadError, AssetType] =
      implicitly[DynamoFormat[String]].read(av).flatMap(x => AssetType.valueOf(x).toRight[DynamoReadError](TypeCoercionError(new IllegalArgumentException(x + " is not a valid AssetType"))))
    override def write(a: AssetType): AttributeValue = implicitly[DynamoFormat[String]].write(a.name)
  }

  implicit val gcategory = new DynamoFormat[Category] {
    override def read(av: AttributeValue): Either[DynamoReadError, Category] =
      implicitly[DynamoFormat[String]].read(av).flatMap(x => Category.valueOf(x).toRight[DynamoReadError](TypeCoercionError(new IllegalArgumentException(x + " is not a valid Category"))))
    override def write(a: Category): AttributeValue = implicitly[DynamoFormat[String]].write(a.name)
  }

  implicit val gprivacystatus = new DynamoFormat[PrivacyStatus] {
    override def read(av: AttributeValue): Either[DynamoReadError, PrivacyStatus] =
      implicitly[DynamoFormat[String]].read(av).flatMap(x => PrivacyStatus.valueOf(x).toRight[DynamoReadError](TypeCoercionError(new IllegalArgumentException(x + " is not a valid PrivacyStatus"))))
    override def write(a: PrivacyStatus): AttributeValue = implicitly[DynamoFormat[String]].write(a.name)
  }

  implicit val greviewtype = new DynamoFormat[ReviewType] {
    override def read(av: AttributeValue): Either[DynamoReadError, ReviewType] =
      implicitly[DynamoFormat[String]].read(av).flatMap(x => ReviewType.valueOf(x).toRight[DynamoReadError](TypeCoercionError(new IllegalArgumentException(x + " is not a valid ReviewType"))))
    override def write(a: ReviewType): AttributeValue = implicitly[DynamoFormat[String]].write(a.name)
  }

  implicit val grelatedstorylinktype = new DynamoFormat[RelatedStoryLinkType] {
    override def read(av: AttributeValue): Either[DynamoReadError, RelatedStoryLinkType] =
      implicitly[DynamoFormat[String]].read(av).flatMap(x => RelatedStoryLinkType.valueOf(x).toRight[DynamoReadError](TypeCoercionError(new IllegalArgumentException(x + " is not a valid RelatedStoryLinkType"))))
    override def write(a: RelatedStoryLinkType): AttributeValue = implicitly[DynamoFormat[String]].write(a.name)
  }

  implicit val gatomtype = new DynamoFormat[AtomType] {
    override def read(av: AttributeValue): Either[DynamoReadError, AtomType] =
      implicitly[DynamoFormat[String]].read(av).flatMap(x => AtomType.valueOf(x).toRight[DynamoReadError](TypeCoercionError(new IllegalArgumentException(x + " is not a valid AtomType"))))
    override def write(a: AtomType): AttributeValue = implicitly[DynamoFormat[String]].write(a.name)
  }

  implicit val gdisplaytype = new DynamoFormat[DisplayType] {
    override def read(av: AttributeValue): Either[DynamoReadError, DisplayType] =
      implicitly[DynamoFormat[String]].read(av).flatMap(x => DisplayType.valueOf(x).toRight[DynamoReadError](TypeCoercionError(new IllegalArgumentException(x + " is not a valid DisplayType"))))
    override def write(a: DisplayType): AttributeValue = implicitly[DynamoFormat[String]].write(a.name)
  }

  implicit val gentitytype = new DynamoFormat[EntityType] {
    override def read(av: AttributeValue): Either[DynamoReadError, EntityType] =
      implicitly[DynamoFormat[String]].read(av).flatMap(x => EntityType.valueOf(x).toRight[DynamoReadError](TypeCoercionError(new IllegalArgumentException(x + " is not a valid EntityType"))))
    override def write(a: EntityType): AttributeValue = implicitly[DynamoFormat[String]].write(a.name)
  }

  /*** ATOM ***/

  implicit val guser = new LabelledGeneric[User] {
    val t1 = Witness.`'email`
    val t2 = Witness.`'firstName`
    val t3 = Witness.`'lastName`

    type Repr = F[t1.T, String] :: F[t2.T, Option[String]] :: F[t3.T, Option[String]] :: HNil

    def to(a: User): Repr =
      ('email ->> a.email) ::
      ('firstName ->> a.firstName) ::
      ('lastName ->> a.lastName) :: HNil

    def from(r: Repr): User = User(
      r.head,
      r.tail.head,
      r.tail.tail.head
    )
  }

  implicit val gcr = new LabelledGeneric[ChangeRecord] {
    val t1 = Witness.`'date`
    val t2 = Witness.`'user`

    type Repr = F[t1.T, Long] :: F[t2.T, Option[User]] :: HNil

    def to(a: ChangeRecord): Repr =
      ('date ->> a.date) ::
      ('user ->> a.user) :: HNil

    def from(r: Repr): ChangeRecord = ChangeRecord(
      r.head,
      r.tail.head
    )
  }

  implicit val gccd = new LabelledGeneric[ContentChangeDetails] {
    val t1 = Witness.`'lastModified`
    val t2 = Witness.`'created`
    val t3 = Witness.`'published`
    val t4 = Witness.`'revision`
    val t5 = Witness.`'takenDown`

    type Repr = F[t1.T, Option[ChangeRecord]] :: F[t2.T, Option[ChangeRecord]] :: F[t3.T, Option[ChangeRecord]] :: F[t4.T, Long] :: F[t5.T, Option[ChangeRecord]] :: HNil

    def to(a: ContentChangeDetails): Repr =
      ('lastModified ->> a.lastModified) ::
      ('created ->> a.created) ::
      ('published ->> a.published) ::
      ('revision ->> a.revision) ::
      ('takenDown ->> a.takenDown) :: HNil

    def from(r: Repr): ContentChangeDetails = ContentChangeDetails(
      r.head,
      r.tail.head,
      r.tail.tail.head,
      r.tail.tail.tail.head,
      r.tail.tail.tail.tail.head
    )
  }

  implicit val gflags = new LabelledGeneric[Flags] {
    val t1 = Witness.`'legallySensitive`
    val t2 = Witness.`'blockAds`
    val t3 = Witness.`'sensitive`

    type Repr = F[t1.T, Option[Boolean]] :: F[t2.T, Option[Boolean]] :: F[t3.T, Option[Boolean]] :: HNil

    def to(a: Flags): Repr =
      ('legallySensitive ->> a.legallySensitive) ::
      ('blockAds ->> a.blockAds) ::
      ('sensitive ->> a.sensitive) :: HNil

    def from(r: Repr): Flags = Flags(
      r.head,
      r.tail.head,
      r.tail.tail.head
    )
  }

  implicit val gatom = new LabelledGeneric[Atom] {
    val t1 = Witness.`'id`
    val t2 = Witness.`'atomType`
    val t3 = Witness.`'labels`
    val t4 = Witness.`'defaultHtml`
    val t5 = Witness.`'data`
    val t6 = Witness.`'contentChangeDetails`
    val t7 = Witness.`'flags`
    val t8 = Witness.`'title`

    type Repr = F[t1.T, String] :: F[t2.T, AtomType] :: F[t3.T, Seq[String]] :: F[t4.T, String] :: F[t5.T, AtomData] :: F[t6.T, ContentChangeDetails] :: F[t7.T, Option[Flags]] :: F[t8.T, Option[String]] :: HNil

    def to(a: Atom): Repr =
      ('id ->> a.id) ::
      ('atomType ->> a.atomType) ::
      ('labels ->> a.labels) ::
      ('defaultHtml ->> a.defaultHtml) ::
      ('data ->> a.data) ::
      ('contentChangeDetails ->> a.contentChangeDetails) ::
      ('flags ->> a.flags) ::
      ('title ->> a.title) :: HNil

    def from(r: Repr): Atom = Atom(
      r.head,
      r.tail.head,
      r.tail.tail.head,
      r.tail.tail.tail.head,
      r.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.tail.tail.head
    )
  }

  /*** CTA ATOM ***/

  implicit val gcta = new LabelledGeneric[CTAAtom] {
    val t1 = Witness.`'url`
    val t2 = Witness.`'backgroundImage`
    val t3 = Witness.`'btnText`
    val t4 = Witness.`'label`
    val t5 = Witness.`'trackingCode`

    type Repr = F[t1.T, String] :: F[t2.T, Option[String]] :: F[t3.T, Option[String]] :: F[t4.T, Option[String]] :: F[t5.T, Option[String]] :: HNil

    def to(a: CTAAtom): Repr =
      ('url ->> a.url) ::
      ('backgroundImage ->> a.backgroundImage) ::
      ('btnText ->> a.btnText) ::
      ('label ->> a.label) ::
      ('trackingCode ->> a.trackingCode) :: HNil

    def from(r: Repr): CTAAtom = CTAAtom(
      r.head,
      r.tail.head,
      r.tail.tail.head,
      r.tail.tail.tail.head,
      r.tail.tail.tail.tail.head
    )
  }

  /*** EXPLAINER ATOM ***/

  implicit val gexplainer = new LabelledGeneric[ExplainerAtom] {
    val t1 = Witness.`'title`
    val t2 = Witness.`'body`
    val t3 = Witness.`'displayType`
    val t4 = Witness.`'tags`

    type Repr = F[t1.T, String] :: F[t2.T, String] :: F[t3.T, DisplayType] :: F[t4.T, Option[Seq[String]]] :: HNil

    def to(a: ExplainerAtom): Repr =
      ('title ->> a.title) ::
      ('body ->> a.body) ::
      ('displayType ->> a.displayType) ::
      ('tags ->> a.tags) :: HNil

    def from(r: Repr): ExplainerAtom = ExplainerAtom(
      r.head,
      r.tail.head,
      r.tail.tail.head,
      r.tail.tail.tail.head
    )
  }

  /*** INTERACTIVE ATOM ***/

  implicit val ginteractive = new LabelledGeneric[InteractiveAtom] {
    val t1 = Witness.`'type`
    val t2 = Witness.`'title`
    val t3 = Witness.`'css`
    val t4 = Witness.`'html`
    val t5 = Witness.`'mainJS`
    val t6 = Witness.`'docData`

    type Repr = F[t1.T, String] :: F[t2.T, String] :: F[t3.T, String] :: F[t4.T, String] :: F[t5.T, Option[String]] :: F[t6.T, Option[String]] :: HNil

    def to(a: InteractiveAtom): Repr =
      ('type ->> a.`type`) ::
      ('title ->> a.title) ::
      ('css ->> a.css) ::
      ('html ->> a.html) ::
      ('mainJS ->> a.mainJS) ::
      ('docData ->> a.docData) :: HNil

    def from(r: Repr): InteractiveAtom = InteractiveAtom(
      r.head,
      r.tail.head,
      r.tail.tail.head,
      r.tail.tail.tail.head,
      r.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.head
    )
  }

  /*** ENTITIES ***/

  implicit val gimageassetdims = new LabelledGeneric[ImageAssetDimensions] {
    val t1 = Witness.`'height`
    val t2 = Witness.`'width`

    type Repr = F[t1.T, Int] :: F[t2.T, Int] :: HNil

    def to(a: ImageAssetDimensions): Repr =
      ('height ->> a.height) ::
      ('width ->> a.width) :: HNil

    def from(r: Repr): ImageAssetDimensions = ImageAssetDimensions(
      r.head,
      r.tail.head
    )

  }

  implicit val gimageasset = new LabelledGeneric[ImageAsset] {
    val t1 = Witness.`'mimeType`
    val t2 = Witness.`'file`
    val t3 = Witness.`'dimensions`
    val t4 = Witness.`'size`

    type Repr = F[t1.T, Option[String]] :: F[t2.T, String] :: F[t3.T, Option[ImageAssetDimensions]] :: F[t4.T, Option[Long]] :: HNil

    def to(a: ImageAsset): Repr =
      ('mimeType ->> a.mimeType) ::
      ('file ->> a.file) ::
      ('dimensions ->> a.dimensions) ::
      ('size ->> a.size) :: HNil

    def from(r: Repr): ImageAsset = ImageAsset(
      r.head,
      r.tail.head,
      r.tail.tail.head,
      r.tail.tail.tail.head
    )

  }

  implicit val gimage = new LabelledGeneric[Image] {
    val t1 = Witness.`'assets`
    val t2 = Witness.`'master`
    val t3 = Witness.`'mediaId`

    type Repr = F[t1.T, Seq[ImageAsset]] :: F[t2.T, Option[ImageAsset]] :: F[t3.T, String] :: HNil

    def to(a: Image): Repr =
      ('assets ->> a.assets) ::
      ('master ->> a.master) ::
      ('mediaId ->> a.mediaId) :: HNil

    def from(r: Repr): Image = Image(
      r.head,
      r.tail.head,
      r.tail.tail.head
    )

  }

  implicit val gplace =  new LabelledGeneric[Place] {
    val t1 = Witness.`'name`

    type Repr = F[t1.T, String] :: HNil

    def to(a: Place): Repr = ('name ->> a.name) :: HNil

    def from(r: Repr): Place = Place(r.head)
  }

  implicit val gorganisation =  new LabelledGeneric[Organisation] {
    val t1 = Witness.`'name`

    type Repr = F[t1.T, String] :: HNil

    def to(a: Organisation): Repr = ('name ->> a.name) :: HNil

    def from(r: Repr): Organisation = Organisation(r.head)
  }

  implicit val gperson =  new LabelledGeneric[Person] {
    val t1 = Witness.`'fullName`

    type Repr = F[t1.T, String] :: HNil

    def to(a: Person): Repr = ('fullName ->> a.fullName) :: HNil

    def from(r: Repr): Person = Person(r.head)
  }

  implicit val gfilm =  new LabelledGeneric[Film] {
    val t1 = Witness.`'title`
    val t2 = Witness.`'year`
    val t3 = Witness.`'imdbId`
    val t4 = Witness.`'directors`
    val t5 = Witness.`'actors`
    val t6 = Witness.`'genre`

    type Repr = F[t1.T, String] :: F[t2.T, Short] :: F[t3.T, String] :: F[t4.T, Seq[Person]] :: F[t5.T, Seq[Person]] :: F[t6.T, Seq[String]] :: HNil

    def to(a: Film): Repr =
      ('title ->> a.title) ::
      ('year ->> a.year) ::
      ('imdbId ->> a.imdbId) ::
      ('directors ->> a.directors) ::
      ('actors ->> a.actors) ::
      ('genre ->> a.genre) :: HNil

    def from(r: Repr): Film = Film(
      r.head,
      r.tail.head,
      r.tail.tail.head,
      r.tail.tail.tail.head,
      r.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.head
    )
  }

  implicit val gprice =  new LabelledGeneric[Price] {
    val t1 = Witness.`'currency`
    val t2 = Witness.`'value`

    type Repr = F[t1.T, String] :: F[t2.T, Int] :: HNil

    def to(a: Price): Repr =
      ('currency ->> a.currency) ::
      ('value ->> a.value) :: HNil

    def from(r: Repr): Price = Price(
      r.head,
      r.tail.head
    )
  }

  implicit val ggame =  new LabelledGeneric[Game] {
    val t1 = Witness.`'title`
    val t2 = Witness.`'publisher`
    val t3 = Witness.`'platforms`
    val t4 = Witness.`'price`
    val t5 = Witness.`'pegiRating`
    val t6 = Witness.`'genre`

    type Repr = F[t1.T, String] :: F[t2.T, Option[String]] :: F[t3.T, Seq[String]] :: F[t4.T, Option[Price]] :: F[t5.T, Option[Int]] :: F[t6.T, Seq[String]] :: HNil

    def to(a: Game): Repr =
      ('title ->> a.title) ::
      ('publisher ->> a.publisher) ::
      ('platforms ->> a.platforms) ::
      ('price ->> a.price) ::
      ('pegiRating ->> a.pegiRating) ::
      ('genre ->> a.genre) :: HNil

    def from(r: Repr): Game = Game(
      r.head,
      r.tail.head,
      r.tail.tail.head,
      r.tail.tail.tail.head,
      r.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.head
    )
  }

  implicit val glocation =  new LabelledGeneric[Geolocation] {
    val t1 = Witness.`'lat`
    val t2 = Witness.`'lon`

    type Repr = F[t1.T, Double] :: F[t2.T, Double] :: HNil

    def to(a: Geolocation): Repr =
      ('lat ->> a.lat) ::
      ('lon ->> a.lon) :: HNil

    def from(r: Repr): Geolocation = Geolocation(r.head, r.tail.head)
  }

  implicit val gaddress =  new LabelledGeneric[Address] {
    val t1 = Witness.`'formattedAddress`
    val t2 = Witness.`'streetNumber`
    val t3 = Witness.`'streetName`
    val t4 = Witness.`'neighbourhood`
    val t5 = Witness.`'postTown`
    val t6 = Witness.`'locality`
    val t7 = Witness.`'country`
    val t8 = Witness.`'administrativeAreaLevelOne`
    val t9 = Witness.`'administrativeAreaLevelTwo`
    val t10 = Witness.`'postCode`

    type Repr = F[t1.T, Option[String]] :: F[t2.T, Option[Short]] :: F[t3.T, Option[String]] :: F[t4.T, Option[String]] :: F[t5.T, Option[String]] :: F[t6.T, Option[String]] :: F[t7.T, Option[String]] :: F[t8.T, Option[String]] :: F[t9.T, Option[String]] :: F[t10.T, Option[String]] :: HNil

    def to(a: Address): Repr =
      ('formattedAddress ->> a.formattedAddress) ::
      ('streetNumber ->> a.streetNumber) ::
      ('streetName ->> a.streetName) ::
      ('neighbourhood ->> a.neighbourhood) ::
      ('postTown ->> a.postTown) ::
      ('locality ->> a.locality) ::
      ('country ->> a.country) ::
      ('administrativeAreaLevelOne ->> a.administrativeAreaLevelOne) ::
      ('administrativeAreaLevelTwo ->> a.administrativeAreaLevelTwo) ::
      ('postCode ->> a.postCode) :: HNil

    def from(r: Repr): Address = Address(
      r.head,
      r.tail.head,
      r.tail.tail.head,
      r.tail.tail.tail.head,
      r.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.tail.tail.tail.tail.head
    )
  }

  implicit val grestaurant =  new LabelledGeneric[Restaurant] {
    val t1 = Witness.`'restaurantName`
    val t2 = Witness.`'approximateLocation`
    val t3 = Witness.`'webAddress`
    val t4 = Witness.`'address`
    val t5 = Witness.`'geolocation`

    type Repr = F[t1.T, String] :: F[t2.T, Option[String]] :: F[t3.T, Option[String]] :: F[t4.T, Option[Address]] :: F[t5.T, Option[Geolocation]] :: HNil

    def to(a: Restaurant): Repr =
      ('restaurantName ->> a.restaurantName) ::
      ('approximateLocation ->> a.approximateLocation) ::
      ('webAddress ->> a.webAddress) ::
      ('address ->> a.address) ::
      ('geolocation ->> a.geolocation) :: HNil

    def from(r: Repr): Restaurant = Restaurant(
      r.head,
      r.tail.head,
      r.tail.tail.head,
      r.tail.tail.tail.head,
      r.tail.tail.tail.tail.head
    )
  }

  implicit val gentity =  new LabelledGeneric[Entity] {
    val t1 = Witness.`'id`
    val t2 = Witness.`'entityType`
    val t3 = Witness.`'googleId`
    val t4 = Witness.`'person`
    val t5 = Witness.`'film`
    val t6 = Witness.`'game`
    val t7 = Witness.`'restaurant`
    val t8 = Witness.`'place`
    val t9 = Witness.`'organisation`

    type Repr = F[t1.T, String] :: F[t2.T, EntityType] :: F[t3.T, Option[String]] :: F[t4.T, Option[Person]] :: F[t5.T, Option[Film]] :: F[t6.T, Option[Game]] :: F[t7.T, Option[Restaurant]] :: F[t8.T, Option[Place]] :: F[t9.T, Option[Organisation]] :: HNil

    def to(a: Entity): Repr =
      ('id ->> a.id) ::
      ('entityType ->> a.entityType) ::
      ('googleId ->> a.googleId) ::
      ('person ->> a.person) ::
      ('film ->> a.film) ::
      ('game ->> a.game) ::
      ('restaurant ->> a.restaurant) ::
      ('place ->> a.place) ::
      ('organisation ->> a.organisation) :: HNil

    def from(r: Repr): Entity = Entity(
      r.head,
      r.tail.head,
      r.tail.tail.head,
      r.tail.tail.tail.head,
      r.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.tail.tail.tail.head
    )
  }

  /*** REVIEW ATOM ***/

  implicit val grating =  new LabelledGeneric[Rating] {
    val t1 = Witness.`'maxRating`
    val t2 = Witness.`'actualRating`
    val t3 = Witness.`'minRating`

    type Repr = F[t1.T, Short] :: F[t2.T, Short] :: F[t3.T, Short] :: HNil

    def to(a: Rating): Repr =
      ('maxRating ->> a.maxRating) ::
      ('actualRating ->> a.actualRating) ::
      ('minRating ->> a.minRating) :: HNil

    def from(r: Repr): Rating = Rating(
      r.head,
      r.tail.head,
      r.tail.tail.head
    )
  }

  implicit val greview =  new LabelledGeneric[ReviewAtom] {
    val t1 = Witness.`'reviewType`
    val t2 = Witness.`'reviewer`
    val t3 = Witness.`'rating`
    val t4 = Witness.`'reviewSnippet`
    val t5 = Witness.`'entityId`
    val t6 = Witness.`'restaurant`
    val t7 = Witness.`'game`
    val t8 = Witness.`'film`
    val t9 = Witness.`'sourceArticleId`
    val t10 = Witness.`'images`

    type Repr = F[t1.T, ReviewType] :: F[t2.T, String] :: F[t3.T, Rating] :: F[t4.T, String] :: F[t5.T, String] :: F[t6.T, Option[Restaurant]] :: F[t7.T, Option[Game]] :: F[t8.T, Option[Film]] :: F[t9.T, Option[String]] :: F[t10.T, Option[Seq[Image]]] :: HNil

    def to(a: ReviewAtom): Repr =
      ('reviewType ->> a.reviewType) ::
      ('reviewer ->> a.reviewer) ::
      ('rating ->> a.rating) ::
      ('reviewSnippet ->> a.reviewSnippet) ::
      ('entityId ->> a.entityId) ::
      ('restaurant ->> a.restaurant) ::
      ('game ->> a.game) ::
      ('film ->> a.film) ::
      ('sourceArticleId ->> a.sourceArticleId) ::
      ('images ->> a.images) :: HNil

    def from(r: Repr): ReviewAtom = ReviewAtom(
      r.head,
      r.tail.head,
      r.tail.tail.head,
      r.tail.tail.tail.head,
      r.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.tail.tail.tail.tail.head
    )
  }

  /*** RECIPE ATOM ***/

  implicit val gtags =  new LabelledGeneric[Tags] {
    val t1 = Witness.`'cuisine`
    val t2 = Witness.`'category`
    val t3 = Witness.`'celebration`
    val t4 = Witness.`'dietary`

    type Repr = F[t1.T, Seq[String]] :: F[t2.T, Seq[String]] :: F[t3.T, Seq[String]] :: F[t4.T, Seq[String]] :: HNil

    def to(a: Tags): Repr =
      ('cuisine ->> a.cuisine) ::
      ('category ->> a.category) ::
      ('celebration ->> a.celebration) ::
      ('dietary ->> a.dietary) :: HNil

    def from(r: Repr): Tags = Tags(
      r.head,
      r.tail.head,
      r.tail.tail.head,
      r.tail.tail.tail.head
    )
  }

  implicit val gtime =  new LabelledGeneric[Time] {
    val t1 = Witness.`'preparation`
    val t2 = Witness.`'cooking`

    type Repr = F[t1.T, Option[Short]] :: F[t2.T, Option[Short]] :: HNil

    def to(a: Time): Repr =
      ('preparation ->> a.preparation) ::
      ('cooking ->> a.cooking) :: HNil

    def from(r: Repr): Time = Time(
      r.head,
      r.tail.head
    )
  }

  implicit val gserves =  new LabelledGeneric[Serves] {
    val t1 = Witness.`'type`
    val t2 = Witness.`'from`
    val t3 = Witness.`'to`
    val t4 = Witness.`'unit`

    type Repr = F[t1.T, String] :: F[t2.T, Short] :: F[t3.T, Short] :: F[t4.T, Option[String]] :: HNil

    def to(a: Serves): Repr =
      ('type ->> a.`type`) ::
      ('from ->> a.from) ::
      ('to ->> a.to) ::
      ('unit ->> a.unit) :: HNil

    def from(r: Repr): Serves = Serves(
      r.head,
      r.tail.head,
      r.tail.tail.head,
      r.tail.tail.tail.head
    )
  }

  implicit val grange =  new LabelledGeneric[Range] {
    val t1 = Witness.`'from`
    val t2 = Witness.`'to`

    type Repr = F[t1.T, Short] :: F[t2.T, Short] :: HNil

    def to(a: Range): Repr = ('from ->> a.from) :: ('to ->> a.to) :: HNil

    def from(r: Repr): Range = Range(
      r.head,
      r.tail.head
    )
  }

  implicit val gingredient =  new LabelledGeneric[Ingredient] {
    val t1 = Witness.`'item`
    val t2 = Witness.`'comment`
    val t3 = Witness.`'quantity`
    val t4 = Witness.`'quantityRange`
    val t5 = Witness.`'unit`

    type Repr = F[t1.T, String] :: F[t2.T, Option[String]] :: F[t3.T, Option[Double]] :: F[t4.T, Option[Range]] :: F[t5.T, Option[String]] :: HNil

    def to(a: Ingredient): Repr = ('item ->> a.item) ::
      ('comment ->> a.comment) ::
      ('quantity ->> a.quantity) ::
      ('quantityRange ->> a.quantityRange) ::
      ('unit ->> a.unit) :: HNil

    def from(r: Repr): Ingredient = Ingredient(
      r.head,
      r.tail.head,
      r.tail.tail.head,
      r.tail.tail.tail.head,
      r.tail.tail.tail.tail.head
    )
  }

  implicit val gingrelist =  new LabelledGeneric[IngredientsList] {
    val t1 = Witness.`'title`
    val t2 = Witness.`'ingredients`

    type Repr = F[t1.T, Option[String]] :: F[t2.T, Seq[Ingredient]] :: HNil

    def to(a: IngredientsList): Repr = ('title ->> a.title) :: ('ingredients ->> a.ingredients) :: HNil

    def from(r: Repr): IngredientsList = IngredientsList(
      r.head,
      r.tail.head
    )
  }

  implicit val grecipe =  new LabelledGeneric[RecipeAtom] {
    val t1 = Witness.`'title`
    val t2 = Witness.`'tags`
    val t3 = Witness.`'time`
    val t4 = Witness.`'serves`
    val t5 = Witness.`'ingredientsLists`
    val t6 = Witness.`'steps`
    val t7 = Witness.`'credits`
    val t8 = Witness.`'images`
    val t9 = Witness.`'sourceArticleId`

    type Repr = F[t1.T, String] :: F[t2.T, Tags] :: F[t3.T, Time] :: F[t4.T, Option[Serves]] :: F[t5.T, Seq[IngredientsList]] :: F[t6.T, Seq[String]] :: F[t7.T, Seq[String]] :: F[t8.T, Seq[Image]] :: F[t9.T, Option[String]] :: HNil

    def to(a: RecipeAtom): Repr = ('title ->> a.title) ::
      ('tags ->> a.tags) ::
      ('time ->> a.time) ::
      ('serves ->> a.serves) ::
      ('ingredientsLists ->> a.ingredientsLists) ::
      ('steps ->> a.steps) ::
      ('credits ->> a.credits) ::
      ('images ->> a.images) ::
      ('sourceArticleId ->> a.sourceArticleId) :: HNil

    def from(r: Repr): RecipeAtom = RecipeAtom(
      r.head,
      r.tail.head,
      r.tail.tail.head,
      r.tail.tail.tail.head,
      r.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.tail.tail.tail.head
    )
  }

  /*** QUIZ ATOM ***/

  implicit val grgroup =  new LabelledGeneric[ResultGroup] {
    val t1 = Witness.`'title`
    val t2 = Witness.`'share`
    val t3 = Witness.`'minScore`
    val t4 = Witness.`'id`

    type Repr = F[t1.T, String] :: F[t2.T, String] :: F[t3.T, Short] :: F[t4.T, String] :: HNil

    def to(a: ResultGroup): Repr =
      ('title ->> a.title) ::
      ('share ->> a.share) ::
      ('minScore ->> a.minScore) ::
      ('id ->> a.id) :: HNil

    def from(r: Repr): ResultGroup = ResultGroup(
      r.head,
      r.tail.head,
      r.tail.tail.head,
      r.tail.tail.tail.head
    )
  }

  implicit val gquizasset =  new LabelledGeneric[QuizAsset] {
    val t1 = Witness.`'type`
    val t2 = Witness.`'data`

    type Repr = F[t1.T, String] :: F[t2.T, String] :: HNil

    def to(a: QuizAsset): Repr =
      ('type ->> a.`type`) ::
      ('data ->> a.data) :: HNil

    def from(r: Repr): QuizAsset = QuizAsset(
      r.head,
      r.tail.head
    )
  }

  implicit val ganswer =  new LabelledGeneric[Answer] {
    val t1 = Witness.`'answerText`
    val t2 = Witness.`'assets`
    val t3 = Witness.`'weight`
    val t4 = Witness.`'revealText`
    val t5 = Witness.`'id`
    val t6 = Witness.`'bucket`

    type Repr = F[t1.T, String] :: F[t2.T, Seq[QuizAsset]] :: F[t3.T, Short] :: F[t4.T, Option[String]] :: F[t5.T, String] :: F[t6.T, Option[Seq[String]]] :: HNil

    def to(a: Answer): Repr =
      ('answerText ->> a.answerText) ::
      ('assets ->> a.assets) ::
      ('weight ->> a.weight) ::
      ('revealText ->> a.revealText) ::
      ('id ->> a.id) ::
      ('bucket ->> a.bucket) :: HNil

    def from(r: Repr): Answer = Answer(
      r.head,
      r.tail.head,
      r.tail.tail.head,
      r.tail.tail.tail.head,
      r.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.head
    )
  }

  implicit val grbucket =  new LabelledGeneric[ResultBucket] {
    val t1 = Witness.`'assets`
    val t2 = Witness.`'description`
    val t3 = Witness.`'title`
    val t4 = Witness.`'share`
    val t5 = Witness.`'id`

    type Repr = F[t1.T, Option[Seq[QuizAsset]]] :: F[t2.T, String] :: F[t3.T, String] :: F[t4.T, String] :: F[t5.T, String] :: HNil

    def to(a: ResultBucket): Repr =
      ('assets ->> a.assets) ::
      ('description ->> a.description) ::
      ('title ->> a.title) ::
      ('share ->> a.share) ::
      ('id ->> a.id) :: HNil

    def from(r: Repr): ResultBucket = ResultBucket(
      r.head,
      r.tail.head,
      r.tail.tail.head,
      r.tail.tail.tail.head,
      r.tail.tail.tail.tail.head
    )
  }

  implicit val gqquestion =  new LabelledGeneric[QQuestion] {
    val t1 = Witness.`'questionText`
    val t2 = Witness.`'assets`
    val t3 = Witness.`'answers`
    val t4 = Witness.`'id`

    type Repr = F[t1.T, String] :: F[t2.T, Seq[QuizAsset]] :: F[t3.T, Seq[Answer]] :: F[t4.T, String] :: HNil

    def to(a: QQuestion): Repr =
      ('questionText ->> a.questionText) ::
      ('assets ->> a.assets) ::
      ('answers ->> a.answers) ::
      ('id ->> a.id) :: HNil

    def from(r: Repr): QQuestion = QQuestion(
      r.head,
      r.tail.head,
      r.tail.tail.head,
      r.tail.tail.tail.head
    )
  }

  implicit val grbuckets =  new LabelledGeneric[ResultBuckets] {
    val t1 = Witness.`'buckets`

    type Repr = F[t1.T, Seq[ResultBucket]] :: HNil

    def to(a: ResultBuckets): Repr =
      ('buckets ->> a.buckets) :: HNil

    def from(r: Repr): ResultBuckets = ResultBuckets(
      r.head
    )
  }

  implicit val grgroups =  new LabelledGeneric[ResultGroups] {
    val t1 = Witness.`'groups`

    type Repr = F[t1.T, Seq[ResultGroup]] :: HNil

    def to(a: ResultGroups): Repr =
      ('groups ->> a.groups) :: HNil

    def from(r: Repr): ResultGroups = ResultGroups(
      r.head
    )
  }

  implicit val gquizcontent =  new LabelledGeneric[QuizContent] {
    val t1 = Witness.`'questions`
    val t2 = Witness.`'resultGroups`
    val t3 = Witness.`'resultBuckets`

    type Repr = F[t1.T, Seq[QQuestion]] :: F[t2.T, Option[ResultGroups]] :: F[t3.T, Option[ResultBuckets]] :: HNil

    def to(a: QuizContent): Repr =
      ('questions ->> a.questions) ::
      ('resultGroups ->> a.resultGroups) ::
      ('resultBuckets ->> a.resultBuckets) :: HNil

    def from(r: Repr): QuizContent = QuizContent(
      r.head,
      r.tail.head,
      r.tail.tail.head
    )
  }

  implicit val gquiz =  new LabelledGeneric[QuizAtom] {
    val t1 = Witness.`'id`
    val t2 = Witness.`'title`
    val t3 = Witness.`'revealAtEnd`
    val t4 = Witness.`'published`
    val t5 = Witness.`'quizType`
    val t6 = Witness.`'defaultColumns`
    val t7 = Witness.`'content`

    type Repr = F[t1.T, String] :: F[t2.T, String] :: F[t3.T, Boolean] :: F[t4.T, Boolean] :: F[t5.T, String] :: F[t6.T, Option[Short]] :: F[t7.T, QuizContent] :: HNil

    def to(a: QuizAtom): Repr =
      ('id ->> a.id) ::
      ('title ->> a.title) ::
      ('revealAtEnd ->> a.revealAtEnd) ::
      ('published ->> a.published) ::
      ('quizType ->> a.quizType) ::
      ('defaultColumns ->> a.defaultColumns) ::
      ('content ->> a.content) :: HNil

    def from(r: Repr): QuizAtom = QuizAtom(
      r.head,
      r.tail.head,
      r.tail.tail.head,
      r.tail.tail.tail.head,
      r.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.tail.head
    )
  }

  /*** STORYQUESTIONS ATOM ***/

  implicit val gsquestion =  new LabelledGeneric[SQuestion] {
    val t1 = Witness.`'questionId`
    val t2 = Witness.`'questionText`

    type Repr = F[t1.T, String] :: F[t2.T, String] :: HNil

    def to(a: SQuestion): Repr =
      ('questionId ->> a.questionId) ::
      ('questionText ->> a.questionText) :: HNil

    def from(r: Repr): SQuestion = SQuestion(
      r.head,
      r.tail.head
    )
  }

  implicit val gquestionset =  new LabelledGeneric[QuestionSet] {
    val t1 = Witness.`'questionSetId`
    val t2 = Witness.`'questionSetTitle`
    val t3 = Witness.`'questions`

    type Repr = F[t1.T, String] :: F[t2.T, String] :: F[t3.T, Seq[SQuestion]] :: HNil

    def to(a: QuestionSet): Repr =
      ('questionSetId ->> a.questionSetId) ::
      ('questionSetTitle ->> a.questionSetTitle) ::
      ('questions ->> a.questions) :: HNil

    def from(r: Repr): QuestionSet = QuestionSet(
      r.head,
      r.tail.head,
      r.tail.tail.head
    )
  }

  implicit val gstoryquestion =  new LabelledGeneric[StoryQuestionsAtom] {
    val t1 = Witness.`'relatedStoryId`
    val t2 = Witness.`'relatedStoryLinkType`
    val t3 = Witness.`'title`
    val t4 = Witness.`'editorialQuestions`
    val t5 = Witness.`'userQuestions`

    type Repr = F[t1.T, String] :: F[t2.T, RelatedStoryLinkType] :: F[t3.T, String] :: F[t4.T, Option[Seq[QuestionSet]]] :: F[t5.T, Option[Seq[QuestionSet]]] :: HNil

    def to(a: StoryQuestionsAtom): Repr =
      ('relatedStoryId ->> a.relatedStoryId) ::
      ('relatedStoryLinkType ->> a.relatedStoryLinkType) ::
      ('title ->> a.title) ::
      ('editorialQuestions ->> a.editorialQuestions) ::
      ('userQuestions ->> a.userQuestions) :: HNil

    def from(r: Repr): StoryQuestionsAtom = StoryQuestionsAtom(
      r.head,
      r.tail.head,
      r.tail.tail.head,
      r.tail.tail.tail.head,
      r.tail.tail.tail.tail.head
    )
  }

  /*** Q&A ATOM ***/

  implicit val gqaitem =  new LabelledGeneric[QAndAItem] {
    val t1 = Witness.`'title`
    val t2 = Witness.`'body`

    type Repr = F[t1.T, Option[String]] :: F[t2.T, String] :: HNil

    def to(a: QAndAItem): Repr =
      ('title ->> a.title) ::
      ('body ->> a.body) :: HNil

    def from(r: Repr): QAndAItem = QAndAItem(
      r.head,
      r.tail.head
    )
  }

  implicit val gqa =  new LabelledGeneric[QAndAAtom] {
    val t1 = Witness.`'typeLabel`
    val t2 = Witness.`'eventImage`
    val t3 = Witness.`'item`
    val t4 = Witness.`'question`

    type Repr = F[t1.T, Option[String]] :: F[t2.T, Option[Image]] :: F[t3.T, QAndAItem] :: F[t4.T, Option[SQuestion]] :: HNil

    def to(a: QAndAAtom): Repr =
      ('typeLabel ->> a.typeLabel) ::
      ('eventImage ->> a.eventImage) ::
      ('item ->> a.item) ::
      ('question ->> a.question) :: HNil

    def from(r: Repr): QAndAAtom = QAndAAtom(
      r.head,
      r.tail.head,
      r.tail.tail.head,
      r.tail.tail.tail.head
    )
  }

  /*** PROFILE ATOM ***/

  implicit val gprofileitem =  new LabelledGeneric[ProfileItem] {
    val t1 = Witness.`'title`
    val t2 = Witness.`'body`

    type Repr = F[t1.T, Option[String]] :: F[t2.T, String] :: HNil

    def to(a: ProfileItem): Repr =
      ('title ->> a.title) ::
      ('body ->> a.body) :: HNil

    def from(r: Repr): ProfileItem = ProfileItem(
      r.head,
      r.tail.head
    )
  }

  implicit val gprofile =  new LabelledGeneric[ProfileAtom] {
    val t1 = Witness.`'typeLabel`
    val t2 = Witness.`'headshot`
    val t3 = Witness.`'items`
    val t4 = Witness.`'entity`

    type Repr = F[t1.T, Option[String]] :: F[t2.T, Option[Image]] :: F[t3.T, Seq[ProfileItem]] :: F[t4.T, Option[Entity]] :: HNil

    def to(a: ProfileAtom): Repr =
      ('typeLabel ->> a.typeLabel) ::
      ('headshot ->> a.headshot) ::
      ('items ->> a.items) ::
      ('entity ->> a.entity) :: HNil

    def from(r: Repr): ProfileAtom = ProfileAtom(
      r.head,
      r.tail.head,
      r.tail.tail.head,
      r.tail.tail.tail.head
    )
  }

  /*** TIMELINE ATOM ***/

  implicit val gtimelineitem =  new LabelledGeneric[TimelineItem] {
    val t1 = Witness.`'title`
    val t2 = Witness.`'date`
    val t3 = Witness.`'body`
    val t4 = Witness.`'entities`

    type Repr = F[t1.T, String] :: F[t2.T, Long] :: F[t3.T, Option[String]] :: F[t4.T, Option[Seq[Entity]]] :: HNil

    def to(a: TimelineItem): Repr =
      ('title ->> a.title) ::
      ('date ->> a.date) ::
      ('body ->> a.body) ::
      ('entities ->> a.entities) :: HNil

    def from(r: Repr): TimelineItem = TimelineItem(
      r.head,
      r.tail.head,
      r.tail.tail.head,
      r.tail.tail.tail.head
    )
  }

  implicit val gtimeline =  new LabelledGeneric[TimelineAtom] {
    val t1 = Witness.`'typeLabel`
    val t2 = Witness.`'events`

    type Repr = F[t1.T, Option[String]] :: F[t2.T, Seq[TimelineItem]] :: HNil

    def to(a: TimelineAtom): Repr =
      ('typeLabel ->> a.typeLabel) ::
      ('events ->> a.events) :: HNil

    def from(r: Repr): TimelineAtom = TimelineAtom(
      r.head,
      r.tail.head
    )
  }

  /*** GUIDE ATOM ***/

  implicit val gguideitem =  new LabelledGeneric[GuideItem] {
    val t1 = Witness.`'title`
    val t2 = Witness.`'body`
    val t3 = Witness.`'entities`

    type Repr = F[t1.T, Option[String]] :: F[t2.T, String] :: F[t3.T, Option[Seq[Entity]]] :: HNil

    def to(a: GuideItem): Repr =
      ('title ->> a.title) ::
      ('body ->> a.body) ::
      ('entities ->> a.entities) :: HNil

    def from(r: Repr): GuideItem = GuideItem(
      r.head,
      r.tail.head,
      r.tail.tail.head
    )
  }

  implicit val gguide =  new LabelledGeneric[GuideAtom] {
    val t1 = Witness.`'typeLabel`
    val t2 = Witness.`'guideImage`
    val t3 = Witness.`'items`

    type Repr = F[t1.T, Option[String]] :: F[t2.T, Option[Image]] :: F[t3.T, Seq[GuideItem]] :: HNil

    def to(a: GuideAtom): Repr =
      ('typeLabel ->> a.typeLabel) ::
      ('guideImage ->> a.guideImage) ::
      ('items ->> a.items) :: HNil

    def from(r: Repr): GuideAtom = GuideAtom(
      r.head,
      r.tail.head,
      r.tail.tail.head
    )
  }

  /*** MEDIA ATOM ***/

  implicit val gpluto =  new LabelledGeneric[PlutoData] {
    val t1 = Witness.`'commissionId`
    val t2 = Witness.`'projectId`
    val t3 = Witness.`'masterId`

    type Repr = F[t1.T, Option[String]] :: F[t2.T, Option[String]] :: F[t3.T, Option[String]] :: HNil

    def to(a: PlutoData): Repr =
      ('commissionId ->> a.commissionId) ::
      ('projectId ->> a.projectId) ::
      ('masterId ->> a.masterId) :: HNil

    def from(r: Repr): PlutoData = PlutoData(
      r.head,
      r.tail.head,
      r.tail.tail.head
    )
  }

  implicit val gmetadata =  new LabelledGeneric[Metadata] {
    val t1 = Witness.`'tags`
    val t2 = Witness.`'categoryId`
    val t3 = Witness.`'license`
    val t4 = Witness.`'commentsEnabled`
    val t5 = Witness.`'channelId`
    val t6 = Witness.`'privacyStatus`
    val t7 = Witness.`'expiryDate`
    val t8 = Witness.`'pluto`

    type Repr = F[t1.T, Option[Seq[String]]] :: F[t2.T, Option[String]] :: F[t3.T, Option[String]] :: F[t4.T, Option[Boolean]] :: F[t5.T, Option[String]] :: F[t6.T, Option[PrivacyStatus]] :: F[t7.T, Option[Long]] :: F[t8.T, Option[PlutoData]] :: HNil

    def to(a: Metadata): Repr =
      ('tags ->> a.tags) ::
      ('categoryId ->> a.categoryId) ::
      ('license ->> a.license) ::
      ('commentsEnabled ->> a.commentsEnabled) ::
      ('channelId ->> a.channelId) ::
      ('privacyStatus ->> a.privacyStatus) ::
      ('expiryDate ->> a.expiryDate) ::
      ('pluto ->> a.pluto) :: HNil

    def from(r: Repr): Metadata = Metadata(
      r.head,
      r.tail.head,
      r.tail.tail.head,
      r.tail.tail.tail.head,
      r.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.tail.tail.head
    )
  }

  implicit val gmediasset =  new LabelledGeneric[MediaAsset] {
    val t1 = Witness.`'assetType`
    val t2 = Witness.`'version`
    val t3 = Witness.`'id`
    val t4 = Witness.`'platform`
    val t5 = Witness.`'mimeType`

    type Repr = F[t1.T, AssetType] :: F[t2.T, Long] :: F[t3.T, String] :: F[t4.T, Platform] :: F[t5.T, Option[String]] :: HNil

    def to(a: MediaAsset): Repr =
      ('assetType ->> a.assetType) ::
      ('version ->> a.version) ::
      ('id ->> a.id) ::
      ('platform ->> a.platform) ::
      ('mimeType ->> a.mimeType) :: HNil

    def from(r: Repr): MediaAsset = MediaAsset(
      r.head,
      r.tail.head,
      r.tail.tail.head,
      r.tail.tail.tail.head,
      r.tail.tail.tail.tail.head
    )
  }

  implicit val gmedia =  new LabelledGeneric[MediaAtom] {
    val t1 = Witness.`'assets`
    val t2 = Witness.`'activeVersion`
    val t3 = Witness.`'title`
    val t4 = Witness.`'category`
    val t5 = Witness.`'plutoProjectId`
    val t6 = Witness.`'duration`
    val t7 = Witness.`'source`
    val t8 = Witness.`'posterUrl`
    val t9 = Witness.`'description`
    val t10 = Witness.`'metadata`
    val t11 = Witness.`'posterImage`
    val t12 = Witness.`'trailText`

    type Repr = F[t1.T, Seq[MediaAsset]] :: F[t2.T, Option[Long]] :: F[t3.T, String] :: F[t4.T, Category] :: F[t5.T, Option[String]] :: F[t6.T, Option[Long]] :: F[t7.T, Option[String]] :: F[t8.T, Option[String]] :: F[t9.T, Option[String]] :: F[t10.T, Option[Metadata]] :: F[t11.T, Option[Image]] :: F[t12.T, Option[String]] :: HNil

    def to(a: MediaAtom): Repr =
      ('assets ->> a.assets) ::
      ('activeVersion ->> a.activeVersion) ::
      ('title ->> a.title) ::
      ('category ->> a.category) ::
      ('plutoProjectId ->> a.plutoProjectId) ::
      ('duration ->> a.duration) ::
      ('source ->> a.source) ::
      ('posterUrl ->> a.posterUrl) ::
      ('description ->> a.description) ::
      ('metadata ->> a.metadata) ::
      ('posterImage ->> a.posterImage) ::
      ('trailText ->> a.trailText) :: HNil

    def from(r: Repr): MediaAtom = MediaAtom(
      r.head,
      r.tail.head,
      r.tail.tail.head,
      r.tail.tail.tail.head,
      r.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.tail.tail.tail.tail.tail.head,
      r.tail.tail.tail.tail.tail.tail.tail.tail.tail.tail.tail.head
    )
  }

  /**
    * AtomData -
    * The `data` field is a thrift union type (AtomData), which we serialize to e.g.
    * data: {
    *   profile: {
    *     items: []
    *   }
    * }
    *
    * It is necessary to specify the atom type here, as some types can be indistinguishable.
    *
    */

  implicit val atomData = new LabelledGeneric[AtomData] {
    val t1 = Witness.`'quiz`
    val t2 = Witness.`'media`
    val t3 = Witness.`'explainer`
    val t4 = Witness.`'cta`
    val t5 = Witness.`'interactive`
    val t6 = Witness.`'review`
    val t7 = Witness.`'recipe`
    val t8 = Witness.`'storyquestions`
    val t9 = Witness.`'qanda`
    val t10 = Witness.`'guide`
    val t11 = Witness.`'profile`
    val t12 = Witness.`'timeline`

    import com.gu.contentatom.thrift.AtomData._
    import com.gu.contentatom.thrift.AtomDataAliases._

    type Repr = F[t1.T, Option[QuizAlias]] :: F[t2.T, Option[MediaAlias]] :: F[t3.T, Option[ExplainerAlias]] :: F[t4.T, Option[CtaAlias]] :: F[t5.T, Option[InteractiveAlias]] :: F[t6.T, Option[ReviewAlias]] :: F[t7.T, Option[RecipeAlias]] :: F[t8.T, Option[StoryquestionsAlias]] :: F[t9.T, Option[QandaAlias]] :: F[t10.T, Option[GuideAlias]] :: F[t11.T, Option[ProfileAlias]] :: F[t12.T, Option[TimelineAlias]] :: HNil

    def to(atomData: AtomData): Repr = {
      atomData match {
        case Quiz(data) => buildHList(quiz = Some(data))
        case Media(data) => buildHList(media = Some(data))
        case Explainer(data) => buildHList(explainer = Some(data))
        case Cta(data) => buildHList(cta = Some(data))
        case Interactive(data) => buildHList(interactive = Some(data))
        case Review(data) => buildHList(review = Some(data))
        case Recipe(data) => buildHList(recipe = Some(data))
        case Storyquestions(data) => buildHList(storyquestions = Some(data))
        case Qanda(data) => buildHList(qanda = Some(data))
        case Guide(data) => buildHList(guide = Some(data))
        case Profile(data) => buildHList(profile = Some(data))
        case Timeline(data) => buildHList(timeline = Some(data))
      }
    }

    def from(r: Repr): AtomData = {
      r match {
        case quiz :: media :: explainer :: cta :: interactive :: review :: recipe :: storyquestions :: qanda :: guide :: profile :: timeline :: HNil =>
          quiz.map(Quiz)
            .orElse(media.map(Media))
            .orElse(explainer.map(Explainer))
            .orElse(cta.map(Cta))
            .orElse(interactive.map(Interactive))
            .orElse(review.map(Review))
            .orElse(recipe.map(Recipe))
            .orElse(storyquestions.map(Storyquestions))
            .orElse(qanda.map(Qanda))
            .orElse(guide.map(Guide))
            .orElse(profile.map(Profile))
            .orElse(timeline.map(Timeline))
            .getOrElse(sys.error(s"Error deserializing AtomData, is there a missing atom type in AtomFacade?: $r"))
      }
    }

    private def buildHList(quiz: Option[QuizAlias] = Option.empty[QuizAlias], media: Option[MediaAlias] = Option.empty[MediaAlias], explainer: Option[ExplainerAlias] = Option.empty[ExplainerAlias], cta: Option[CtaAlias] = Option.empty[CtaAlias], interactive: Option[InteractiveAlias] = Option.empty[InteractiveAlias], review: Option[ReviewAlias] = Option.empty[ReviewAlias], recipe: Option[RecipeAlias] = Option.empty[RecipeAlias], storyquestions: Option[StoryquestionsAlias] = Option.empty[StoryquestionsAlias], qanda: Option[QandaAlias] = Option.empty[QandaAlias], guide: Option[GuideAlias] = Option.empty[GuideAlias], profile: Option[ProfileAlias] = Option.empty[ProfileAlias], timeline: Option[TimelineAlias] = Option.empty[TimelineAlias]): Repr = {
      ('quiz ->> quiz) :: ('media ->> media) :: ('explainer ->> explainer) :: ('cta ->> cta) :: ('interactive ->> interactive) :: ('review ->> review) :: ('recipe ->> recipe) :: ('storyquestions ->> storyquestions) :: ('qanda ->> qanda) :: ('guide ->> guide) :: ('profile ->> profile) :: ('timeline ->> timeline) :: HNil
    }
  }
}
