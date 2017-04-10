# Atom maker libraries

This repository contains two libraries for creating, publishing and managing content atoms.

You should first define your new content atom model by adding it [here] (https://github.com/guardian/content-atom).
For the atoms to appear in capi you will also have to make necessary changes to capi (this process will be simplified and documented soon).

An example of a project that uses these libraries to manage atoms can be found [here] (https://github.com/guardian/media-atom-maker).

## Draft vs Complete Atoms
It is possible to save incomplete atoms in a draft data store. This is done by using the automatically generated Draft data type.
The `Draft` type has all the same fields as an Atom but all fields are now optional to allow a work in progress save to a separate data store. 
These drafts only ever live in a data store so there is not the functionality to publish or reindex them as only complete atoms can
be published or reindexed. Implementing a draft data store in your project is optional. The draft data types are generated from the [content atom thrift
definition](https://github.com/guardian/content-atom) using the [Scrooge code gen sbt plugin](https://github.com/guardian/scrooge-code-gen-sbt-plugin).

## Atom-publisher-lib ![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.gu/atom-publisher-lib_2.11/badge.svg)
- Provides traits to include in your application

### PublishedStore, PreviewDataStore and DraftDataStore
- It is only necessary to implement DraftDataStore if you want to save incomplete atoms.
- Functionality for getting, creating, listing and updating atoms
- Functionality for getting, creating, listing and updating drafts
- You will need to provide these with a AmazonDynamoDBClient and the names of your published and preview
dynamo tables, and your new content atom definition.
- If you will be using a draft data store you need to provide it with an AmazonDynamoDBClient and the name
 of your draft dynamo table.
- The data stores rely on implicits so you will need to include:
```
import com.gu.scanamo.scrooge.ScroogeDynamoFormat._
import com.gu.atom.data.AtomDynamoFormats._
import cats.syntax.either._
```
Otherwise you will get a `could not find implicit value` error.

#### Implementation with Compile Time Dependency Injection
```
import com.gu.atom.data._
import com.gu.scanamo.scrooge.ScroogeDynamoFormat._
import com.gu.atom.data.AtomDynamoFormats._
import cats.syntax.either._
import config.Config

object DataStores {
val publishedDataStore = new PublishedDynamoDataStore(Config.dynamoClient, Config.publishedDynamoTableName)
val previewDataStore = new PreviewDynamoDataStore(Config.dynamoClient, Config.dynamoTableName)
val draftDataStore = new DraftDynamoDataStore(Config.dynamoClient, Config.draftDynamoTableName)
}
```


#### Examples of use:

Get a published atom
```
publishedDataStore.getAtom(id) match {
  case Right(atom) => //Do something with the published atom
  case Left(e) => //Handle not finding published atom
}
```

Create a new atom
```
try {
  val result = datastore.createAtom(buildKey(atomType, atom.id), atom)
  result match {
    case Right(atom) => //Do something with atom
    case Left(e) => //Handle creation error
  }
} catch {
  case e: Exception => processException(e)
}

```

### LiveAtomPublisher and PreviewAtomPublisher
- Publishes content atoms to live or preview kinesis streams
- You will need to provide these with a kinesis client and the names of live and preview kinesis streams
you want to publish to and a kinesis client

#### Implementation with Compile Time Dependency Injection
```
import com.gu.atom.publish._
import config.Config

object Publisher {
    val liveKinesisAtomPublisher = new LiveKinesisAtomPublisher(Config.liveKinesisStreamName, Config.kinesisClient)
    val previewKinesisAtomPublisher = new PreviewKinesisAtomPublisher(Config.previewKinesisStreamName, Config.kinesisClient)
}
```

#### Examples of use
```
import com.gu.contentatom.thrift.{ContentAtomEvent, EventType, Atom}

//ContentAtomEvent and Atom models are provided by the content atom library
val atom: Atom = functionToParseAtomFromData(data)
val event = ContentAtomEvent(atom, EvenType.Update, now())
previewPublisher.publishAtomEvent(event) match {
  case Success(_)  => //Handle publishing successfully
  case Failure(err) => //Handle error in publishing
}
```


### PreviewAtomReindexer and PublishedAtomReindexer
- Used to reindex preview and live atoms
- You will need to provide these with a kinesis client, and live and preview reindex kinesis stream name, which are usually
the same as the live and preview kinesis streams.
- `atom-manager-play-lib` provides a reindex controller utilising these (see below).

#### Implementation with Compile Time Dependency Injection
```
import com.gu.atom.publish._
import config.Config

object ReindexDataStores {
  val reindexPreview: PreviewKinesisAtomReindexer =
    new PreviewKinesisAtomReindexer(Config.previewReindexKinesisStreamName, Config.kinesisClient)

  val reindexPublished: PublishedKinesisAtomReindexer =
    new PublishedKinesisAtomReindexer(Config.liveReindexKinesisStreamName, Config.kinesisClient)
}

```

## Atom-manager-play-lib ![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.gu/atom-manager-play_2.11/badge.svg)
- Sits on top of `atom-publisher-lib`
- Provides methods for publishing and reindexing atoms

### Reindexing
- You can use the reindex controller to create and view reindex jobs:

```
POST    /reindex-preview                com.gu.atom.play.ReindexController.newPreviewReindexJob()
POST    /reindex-publish                com.gu.atom.play.ReindexController.newPublishedReindexJob()
GET     /reindex-preview                com.gu.atom.play.ReindexController.previewReindexJobStatus()
GET     /reindex-publish                com.gu.atom.play.ReindexController.publishedReindexJobStatus()
```
The ReindexController also needs to be implemented and added to Routes in your AppComponents instantiation.
```
lazy val router = new Routes(reindex)

lazy val reindex = new ReindexController(
    previewDataStore,
    publishedDataStore,
    reindexPreview,
    reindexPublished,
    Configuration(config),
    actorSystem)
```

### Publishing
- Provides a method for publishing the atom is in the `AtomAPIActions` trait.
- This method will publish your atom in your live kinesis stream and save it in your
published atoms dynamo table.

In your controller:

#### Implementation with Compile Time Dependency Injection
```
package controllers

import play.api.mvc._
import com.gu.atom.play._

//The data stores and publishers and provided by `atom-publisher-lib` library and are used by AtomAPIActions

class MyAtomController (val previewDataStore: PreviewDataStore,
                        val publishedDataStore: PublishedDataStore,
                        val livePublisher: LiveAtomPublisher,
                        val previewPublisher: PreviewAtomPublisher)
    extends AtomAPIActions
```

In your routes file:
```
POST    /api/atom/:id/publish           controllers.MyAtomController.publishAtom(id)
```

