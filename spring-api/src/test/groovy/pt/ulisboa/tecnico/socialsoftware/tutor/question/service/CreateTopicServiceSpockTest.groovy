package pt.ulisboa.tecnico.socialsoftware.tutor.question.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import pt.ulisboa.tecnico.socialsoftware.tutor.course.Course
import pt.ulisboa.tecnico.socialsoftware.tutor.course.CourseRepository
import pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.ExceptionError
import pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.TutorException
import pt.ulisboa.tecnico.socialsoftware.tutor.question.QuestionService
import pt.ulisboa.tecnico.socialsoftware.tutor.question.TopicService
import pt.ulisboa.tecnico.socialsoftware.tutor.question.domain.Topic
import pt.ulisboa.tecnico.socialsoftware.tutor.question.dto.TopicDto
import pt.ulisboa.tecnico.socialsoftware.tutor.question.repository.TopicRepository
import spock.lang.Specification

@DataJpaTest
class CreateTopicServiceSpockTest extends Specification {
    public static final String COURSE_NAME = "Arquitetura de Software"
    public static final String NAME = 'name'

    @Autowired
    TopicService topicService

    @Autowired
    CourseRepository courseRepository

    @Autowired
    TopicRepository topicRepository

    def course

    def setup() {
        course = new Course()
        course.setName(COURSE_NAME)
        courseRepository.save(course)
    }

    def "create a topic"() {
        given:
        def topicDto = new TopicDto()
        topicDto.setName(NAME)

        when:
        topicService.createTopic(COURSE_NAME, topicDto)

        then: "the topic is inside the repository"
        topicRepository.count() == 1L
        def result = topicRepository.findAll().get(0)
        result.getId() != null
        result.getName() == NAME
    }

    def "create a topic with the same name"() {
        given: "createQuestion a question"
        Topic topic = new Topic()
        topic.setName(NAME)
        topic.setCourse(course)
        course.addTopic(topic)
        topicRepository.save(topic)
        and: 'topic dto'
        def topicDto = new TopicDto()
        topicDto.setName(NAME)

        when: 'createQuestion another with the same name'
        topicService.createTopic(COURSE_NAME, topicDto)

        then: "an error occurs"
        def exception = thrown(TutorException)
        exception.error == ExceptionError.DUPLICATE_TOPIC
    }

    @TestConfiguration
    static class QuestionServiceImplTestContextConfiguration {
        @Bean
        QuestionService questionService() {
            return new QuestionService()
        }

        @Bean
        TopicService topicService() {
            return new TopicService()
        }
    }

}
