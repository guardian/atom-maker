package com.gu.atom.data

import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.model.{AttributeDefinition, CreateTableRequest, KeySchemaElement, KeyType, ScalarAttributeType}

import java.net.URI
import scala.jdk.CollectionConverters._

/*
 * copied from:
 *    https://github.com/guardian/scanamo/blob/master/src/test/scala/com/gu/scanamo/LocalDynamoDB.scala
 */

object LocalDynamoDBV2 {
  def client() = {
    DynamoDbAsyncClient.builder()
      .credentialsProvider(StaticCredentialsProvider.create(
        AwsBasicCredentials.create("key", "secret")
      ))
      .endpointOverride(URI.create("http://localhost:8000"))
      .region(Region.EU_WEST_1)
      .build()
  }

  def createTable(client: DynamoDbAsyncClient)(tableName: String)(attributes: (KeyType, String)*) = {
    val attrs = attributes.toList.map { case(kt, attrName) => KeySchemaElement.builder().keyType(kt).attributeName(attrName).build()}
    val attributeDefinitions = attributes.toList.map { case(at, attrName) => AttributeDefinition.builder().attributeType(ScalarAttributeType.S).attributeName(attrName).build() }

    val createTableRequest = CreateTableRequest.builder()
      .tableName(tableName)
      .keySchema(attrs.asJava)
      .attributeDefinitions(attributeDefinitions.asJava)
      .build()
    client.createTable(createTableRequest)
  }



//  private def keySchema(attributes: Seq[(Symbol, ScalarAttributeType)]) = {
//    val hashKeyWithType :: rangeKeyWithType = attributes.toList
//    val keySchemas = hashKeyWithType._1 -> KeyType.HASH :: rangeKeyWithType.map(_._1 -> KeyType.RANGE)
//    keySchemas.map{ case (symbol, keyType) => new KeySchemaElement(symbol.name, keyType)}.asJava
//  }
//
//  private def attributeDefinitions(attributes: Seq[(Symbol, ScalarAttributeType)]) = {
//    attributes.map{ case (symbol, attributeType) => new AttributeDefinition(symbol.name, attributeType)}.asJava
//  }
//
//  private val arbitraryThroughputThatIsIgnoredByDynamoDBLocal = new ProvisionedThroughput(1L, 1L)
}
