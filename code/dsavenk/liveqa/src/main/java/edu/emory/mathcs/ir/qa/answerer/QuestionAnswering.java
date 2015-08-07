package edu.emory.mathcs.ir.qa.answerer;

import edu.emory.mathcs.ir.qa.question.Question;
import edu.emory.mathcs.ir.qa.text.Text;

/**
 * A general interface for question answering.
 */
public interface QuestionAnswering {
    Text GetAnswer(Question question);
}
