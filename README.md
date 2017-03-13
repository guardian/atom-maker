#Atom maker libraries

This repository contains two libraries for creating, publishing and managing content atoms.

You should first define your new content atom model by adding it [here] (https://github.com/guardian/content-atom).
For the atoms to appear in capi you will also have to make necessary changes to capi (this process will be simplified and documented soon).

An example of a project that uses these libraries to manage atoms can be found [here] (https://github.com/guardian/media-atom-maker).

##Atom-publisher-lib ![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.gu/atom-publisher-lib_2.11/badge.svg)
- Provides that traits you an inject in your application

###PublishedStore and PreviewDataStore
- Functionality for getting, creating, listing and updating atoms
- You will need to provide these with a AmazonDynamoDBClient and the names of your published and preview
dynamo tables, and your new content atom definition.

```
import com.gu.contentatom.thrift._
import com.gu.contentatom.thrift.atom.myAtom._

class PublishedMyAtomDataStoreProvider @Inject() (myConfig: AWSConfig)
    extends Provider[PublishedDataStore] {
        def get = new PublishedDynamoDataStore(myConfig.dynamoClient, myConfig.publishedDynamoTableName)
}

class PreviewMyAtomDataStoreProvider @Inject() (awsConfig: AWSConfig)
    extends Provider[PreviewDataStore] {
        def get = new PreviewDynamoDataStore(myConfig.dynamoClient, myConfig.dynamoTableName)
}
```

```
bind(classOf[PublishedDataStore])
.toProvider(classOf[PublishedMyAtomDataStoreProvider])

bind(classOf[PreviewDataStore])
.toProvider(classOf[PreviewMyAtomDataStoreProvider])
```


####Examples of use:

Get a published atom
```
publishedDataStore.getAtom(id) match {
    case Some(atom) => //Do something with the published atom
    case None => //Handle not finding published atom
}
```

Create a new atom
```
previewDataStore.createAtom(atom).fold(
    {
      case IDConflictError => //Handle id conflict error
      case _ => //Handle unknown error
    },
    _ => //Do someting with the new atom

```

###LiveAtomPublisher and PreviewAtomPublisher
- Publishes content atoms to live or preview kinesis streams
- You will need to provide these with a kinesis client and the names of live and preview kinesis streams
you want to publish to and a kinesis client

```
import javax.inject.{Inject, Provider}

import com.gu.atom.publish._

class LiveAtomPublisherProvider @Inject() (myConfig)
  extends Provider[LiveAtomPublisher] {
    def get() = new LiveKinesisAtomPublisher(myConfig.liveKinesisStreamName, myConfig.kinesisClient)
}

  class PreviewAtomPublisherProvider @Inject() ()
    extends Provider[PreviewAtomPublisher] {
      def get() = new PreviewKinesisAtomPublisher(myConfig.previewKinesisStreamName, myConfig.kinesisClient)
}
```

```
bind(classOf[LiveAtomPublisher])
.toProvider(classOf[LiveAtomPublisherProvider])

bind(classOf[PreviewAtomPublisher])
.toProvider(classOf[PreviewAtomPublisherProvider])
```

####Examples of use
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


###PreviewAtomReindexer and PublishedAtomReindexer
- Used to reindex preview and live atoms
- You will need to provide these with a kinesis client, and live and preview reindex kinesis stream name, which are usually
the same as the live and preview kinesis streams.
- `atom-manager-play-lib` provides a reindex controller utilising these (see below).

```
class PreviewAtomReindexerProvider @Inject() (myConfig)
    extends Provider[PreviewAtomReindexer] {
        def get() = new PreviewKinesisAtomReindexer(
            myConfig.previewKinesisReindexStreamName, myConfig.kinesisClient
        )
}

class PublishedAtomReindexerProvider @Inject() (myConfig)
    extends Provider[PublishedAtomReindexer] {
        def get() = new PublishedKinesisAtomReindexer(
            myConfig.publishedKinesisReindexStreamName, myConfig.kinesisClient
        )
}

```

```
bind(classOf[PreviewAtomReindexer])
.toProvider(classOf[PreviewAtomReindexerProvider])

bind(classOf[PublishedAtomReindexer])
.toProvider(classOf[PublishedAtomReindexerProvider])
```
##Atom-manager-play-lib ![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.gu/atom-manager-play_2.11/badge.svg)
- Sits on top of `atom-publisher-lib`
- Provides methods for publishing and reindexing atoms

###Reindexing
- You can use the reindex controller to create and view reindex jobs:

```
POST    /reindex-preview                com.gu.atom.play.ReindexController.newPreviewReindexJob()
POST    /reindex-publish                com.gu.atom.play.ReindexController.newPublishedReindexJob()
GET     /reindex-preview                com.gu.atom.play.ReindexController.previewReindexJobStatus()
GET     /reindex-publish                com.gu.atom.play.ReindexController.publishedReindexJobStatus()
```

###Publishing
- Provides a method for publishing the atom is in the `AtomAPIActions` trait.
- This method will publish your atom in your live kinesis stream and save it in your
published atoms dynamo table.

In your controller:

```
package controllers

import play.api.mvc._
import com.gu.atom.play._

//The data stores and publishers and provided by `atom-publisher-lib` library and are used by AtomAPIActions

class MyAtomController @Inject()  (val previewDataStore: PreviewDataStore,
                                   val publishedDataStore: PublishedDataStore,
                                   val livePublisher: LiveAtomPublisher,
                                   val previewPublisher: PreviewAtomPublisher)
    extends AtomAPIActions
```

In your routes file:
```
POST    /api/atom/:id/publish           controllers.MyAtomController.publishAtom(id)
```

