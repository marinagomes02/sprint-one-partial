package pt.ulisboa.tecnico.socialsoftware.tutor.impexp.domain;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import pt.ulisboa.tecnico.socialsoftware.tutor.course.CourseExecutionRepository;
import pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.TutorException;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.domain.Question;
import pt.ulisboa.tecnico.socialsoftware.tutor.question.repository.QuestionRepository;
import pt.ulisboa.tecnico.socialsoftware.tutor.quiz.QuizService;
import pt.ulisboa.tecnico.socialsoftware.tutor.quiz.domain.Quiz;
import pt.ulisboa.tecnico.socialsoftware.tutor.quiz.domain.QuizQuestion;
import pt.ulisboa.tecnico.socialsoftware.tutor.quiz.dto.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.quiz.dto.QuizQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.tutor.quiz.repository.QuizQuestionRepository;

import java.io.*;
import java.nio.charset.Charset;

import static pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.ExceptionError.QUESTION_NOT_FOUND;
import static pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.ExceptionError.QUIZZES_IMPORT_ERROR;

public class QuizzesXmlImport {
	private QuizService quizService;
	private QuestionRepository questionRepository;
	private QuizQuestionRepository quizQuestionRepository;
    private CourseExecutionRepository courseExecutionRepository;

	public void importQuizzes(InputStream inputStream, QuizService quizService, QuestionRepository questionRepository, QuizQuestionRepository quizQuestionRepository, CourseExecutionRepository courseExecutionRepository) {
		this.quizService = quizService;
		this.questionRepository = questionRepository;
		this.quizQuestionRepository = quizQuestionRepository;
		this.courseExecutionRepository = courseExecutionRepository;

		SAXBuilder builder = new SAXBuilder();
		builder.setIgnoringElementContentWhitespace(true);

		Document doc;
		try {
			Reader reader = new InputStreamReader(inputStream, Charset.defaultCharset());
			doc = builder.build(reader);
		} catch (FileNotFoundException e) {
			throw new TutorException(QUIZZES_IMPORT_ERROR, "File not found");
		} catch (JDOMException e) {
			throw new TutorException(QUIZZES_IMPORT_ERROR, "Coding problem");
		} catch (IOException e) {
			throw new TutorException(QUIZZES_IMPORT_ERROR, "File type or format");
		}

		if (doc == null) {
			throw new TutorException(QUIZZES_IMPORT_ERROR, "File not found ot format error");
		}

		importQuizzes(doc);
	}

	public void importQuizzes(String quizzesXml, QuizService quizService, QuestionRepository questionRepository, QuizQuestionRepository quizQuestionRepository, CourseExecutionRepository courseExecutionRepository) {
		SAXBuilder builder = new SAXBuilder();
		builder.setIgnoringElementContentWhitespace(true);

		InputStream stream = new ByteArrayInputStream(quizzesXml.getBytes());

		importQuizzes(stream, quizService, questionRepository, quizQuestionRepository, courseExecutionRepository);
	}

	private void importQuizzes(Document doc) {
		XPathFactory xpfac = XPathFactory.instance();
		XPathExpression<Element> xp = xpfac.compile("//quizzes/quiz", Filters.element());
		for (Element element : xp.evaluate(doc)) {
			importQuiz(element);
		}
	}

	private void importQuiz(Element quizElement) {
        String acronym = quizElement.getAttributeValue("acronym");
        String academicTerm = quizElement.getAttributeValue("academicTerm");

		Integer number = Integer.valueOf(quizElement.getAttributeValue("number"));
		boolean scramble = false;
		if (quizElement.getAttributeValue("scramble") != null) {
			scramble = Boolean.parseBoolean(quizElement.getAttributeValue("scramble"));
		}
		String title = quizElement.getAttributeValue("title");
		String creationDate = null;
		if (quizElement.getAttributeValue("creationDate") != null) {
            creationDate = quizElement.getAttributeValue("creationDate");
		}

		String availableDate = null;
		if (quizElement.getAttributeValue("availableDate") != null) {
			availableDate = quizElement.getAttributeValue("availableDate");
		}

		String conclusionDate = null;
        if (quizElement.getAttributeValue("conclusionDate") != null) {
            conclusionDate = quizElement.getAttributeValue("conclusionDate");
        }
		Integer year = Integer.valueOf(quizElement.getAttributeValue("year"));
		String type = quizElement.getAttributeValue("type");
		Integer series = null;
		if (quizElement.getAttributeValue("series") != null) {
			series = Integer.valueOf(quizElement.getAttributeValue("series"));
		}
		String version = quizElement.getAttributeValue("version");

		QuizDto quizDto = new QuizDto();
		quizDto.setNumber(number);
		quizDto.setScramble(scramble);
		quizDto.setTitle(title);
		quizDto.setCreationDate(creationDate);
        quizDto.setAvailableDate(availableDate);
        quizDto.setConclusionDate(conclusionDate);
		quizDto.setYear(year);
		quizDto.setType(Quiz.QuizType.valueOf(type));
		quizDto.setSeries(series);
		quizDto.setVersion(version);

		int executionCourseId = this.courseExecutionRepository.findByAcronymAcademicTerm(acronym, academicTerm);
		QuizDto quizDto2 = quizService.createQuiz(executionCourseId, quizDto);
		importQuizQuestions(quizElement.getChild("quizQuestions"), quizDto2);
	}

	private void importQuizQuestions(Element quizQuestionsElement, QuizDto quizDto ) {
		for (Element quizQuestionElement: quizQuestionsElement.getChildren("quizQuestion")) {
			Integer sequence = Integer.valueOf(quizQuestionElement.getAttributeValue("sequence"));
			Integer questionNumber = Integer.valueOf(quizQuestionElement.getAttributeValue("questionNumber"));

			Question question = questionRepository.findByNumber(questionNumber)
					.orElseThrow(() -> new TutorException(QUESTION_NOT_FOUND, questionNumber));

			QuizQuestionDto quizQuestionDto = quizService.addQuestionToQuiz(question.getId(), quizDto.getId());

			QuizQuestion quizQuestion = quizQuestionRepository.findById(quizQuestionDto.getId()).get();

			quizQuestion.setSequence(sequence);
		}
	}
}
