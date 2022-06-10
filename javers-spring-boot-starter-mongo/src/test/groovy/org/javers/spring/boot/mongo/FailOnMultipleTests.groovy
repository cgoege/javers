package org.javers.spring.boot.mongo

import org.bson.Document
import org.javers.core.Javers
import org.javers.repository.jql.QueryBuilder
import org.junit.jupiter.api.Order
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.MongoTemplate
import spock.lang.Specification

@SpringBootTest(classes = [TestApplication])
class FailOnMultipleTests extends Specification{

    @Autowired
    DummyEntityRepository repo

    @Autowired
    Javers javers

    @Autowired
    MongoTemplate mongoTemplate

    def setup() {
        repo.deleteAll()
        mongoTemplate.db.getCollection("jv_snapshots").deleteMany(new Document())
        mongoTemplate.db.getCollection("jv_head_id").deleteMany(new Document())
    }

    /**
     * [Test worker] INFO  org.javers.core.Javers - Commit(id:1.00, snapshots:1, author:unauthenticated, changes - NewObject:1), done in 101 millis (diff:49, persist:52)
     */
    @Order(0)
    def "create entity to induce error on second test"() {
        def entity = repo.save(dummyEntityWithFixedId())
        def changes = javers.findChanges(QueryBuilder.byInstance(entity).build())

        expect:
        changes.size() == 1
    }

    /**
     * [Test worker] INFO  org.javers.core.Javers - Skipping persisting empty commit: Commit(id:1.01, snapshots:0, author:unauthenticated, changes -)
     * [Test worker] INFO  org.javers.core.Javers - Commit(id:1.01, snapshots:0, author:unauthenticated, changes -), done in 4 millis (diff:4, persist:0)
     */
    @Order(1)
    def "fails on all test run because previous test is not cleaned correctly"() {
        def entity = repo.save(dummyEntityWithFixedId())
        def changes = javers.findChanges(QueryBuilder.byInstance(entity).build())

        expect:
        changes.size() == 1

    }

    private def dummyEntityWithFixedId() {
        return new DummyEntity(1)
    }
}
