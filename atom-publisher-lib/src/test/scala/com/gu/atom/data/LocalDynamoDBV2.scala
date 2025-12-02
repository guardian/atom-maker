package com.gu.atom.data

import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.{AttributeDefinition, CreateTableRequest, DeleteTableRequest, DeleteTableResponse, KeySchemaElement, KeyType, ProvisionedThroughput, ScalarAttributeType}

import java.net.URI
import scala.jdk.CollectionConverters._

/*
 * copied from:
 *    https://github.com/guardian/scanamo/blob/master/src/test/scala/com/gu/scanamo/LocalDynamoDB.scala
 */

object LocalDynamoDBV2 {
  def client() = {
    DynamoDbClient.builder()
      .credentialsProvider(StaticCredentialsProvider.create(
        AwsBasicCredentials.create("key", "secret")
      ))
      .endpointOverride(URI.create("http://localhost:8000"))
      .region(Region.EU_WEST_1)
      .build()
  }

  def createTable(client: DynamoDbClient)(tableName: String)(attributes: (KeyType, String)*) = {
    val attrs = attributes.toList.map { case(kt, attrName) => KeySchemaElement.builder().keyType(kt).attributeName(attrName).build()}
    val attributeDefinitions = attributes.toList.map { case(at, attrName) => AttributeDefinition.builder().attributeType(ScalarAttributeType.S).attributeName(attrName).build() }

    val createTableRequest = CreateTableRequest.builder()
      .tableName(tableName)
      .keySchema(attrs.asJava)
      .attributeDefinitions(attributeDefinitions.asJava)
      .provisionedThroughput(ProvisionedThroughput.builder().readCapacityUnits(1L).writeCapacityUnits(1L).build())
      .build()
    client.createTable(createTableRequest)
  }

  def deleteTable(client: DynamoDbClient)(tableName: String): DeleteTableResponse = {
    client.deleteTable(DeleteTableRequest.builder().tableName(tableName).build())
  }

}
