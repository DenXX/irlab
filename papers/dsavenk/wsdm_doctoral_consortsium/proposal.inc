In this work I propose to enrich the input data representation for QA systems by combining available unstructured, semi-structured and unstructured data sources for joint reasoning, which can improve the performance of question answering over both text collections and knowledge bases.

\subsection{Unstructured data QA}
\subsubsection{Using Knowledge Base for text-based QA}

\begin{figure}
\centering
\includegraphics[width=0.5\textwidth]{img/text_kb}
\caption{KB annotation of text}
\label{fig:text_kb}
\end{figure}

For question answering over natural language document collections I propose to extend the text representation with annotations about mentioned entities and their relations from open \cite{Fader:2014:OQA:2623330.2623677} or schema-based knowledge bases (e.g. dbPedia or Freebase).
Such representation allows not only find different mentions of the same entity, but also look into the connections of the mentioned entities in order to learn more about the candidate answer, i.e. whether there is a certain property that is related to phrases in the question, or is there a connection between entities mentioned in the question and what predicate does it relate to.
For example, for the question mentioned in the introduction \textit{``What republican senators supported the nomination of Harriet Miers to the Supreme Court?''} and a candidate answer sentence \textit{``Minority Leader Harry Reid had already offered his open support for Miers.''}, which mentions a senator ``Harry Reid'' the joint text-KB representation looks like Figure \ref{fig:text_kb}.
And a QA system can discover that ``Harry Reid'' political affiliation is with the Democratic Party, and he cannot be referred to as ``republican senator'' as expected in the question.
In other cases using a KB as additional source of information may reveal specific connections between entities in the question and in the answer candidates.
For example, for another TREC QA 2007 question \textit{``For which newspaper does Krugman write?''} and retrieved candidate answer \textit{New York Times} a path between ``Paul Krugman'' and ``New York Times'' in the knowledge graph gives an evidence in support of the candidate.

To do this kind of inference I propose:
\begin{itemize}
\item use existing entity linking approaches to find KB entities mentioned in a question and corresponding answer candidates
\item for each mentioned entities extract a subgraph containing its neighbourhood up to certain number of edges away and paths to other mentioned entities
\item follow maching learning approach for candidate answer ranking and extend the feature representaion with features derived from the subgraph analysis. Example of features are:
	\begin{itemize}
	\item features describing discovered connections between entities mentioned in a question and a candidate answer, such as indicators of the relations, combination of relations with words and n-grams from the questions, similarity between the relations and the question text (using tf-idf or embeddings representation), etc. Textual representation of the predicates in structured knowledge bases can be obtained either from its description or using patterns learned from a large collection using distant supervision \cite{MintzBSJ09}.
	\item features describing the entities mentioned in the answer, i.e. similarities between entity properties and question words, n-grams and phrases, etc.
	\end{itemize}
\end{itemize}

\subsubsection{Using CQA data for text-based QA}
For non-factoid questions it is important to understand the language used in answers to express certain types of information.
Question answering systems benefit from more training data and I propose to use QnA pairs available on CQA websites as a training data for a model, which will learn associations between question intent and words and phrases used in the answer text.
Associations can be learned using word representation in a form of conditional probabilities ($p(w_a|w_{q_1},...,w_{q_n}$) or pointwise mutual information.
Another approach is to employ the deep learning models and information about question and answer phrases with embeddings \cite{WangN15}.


\subsection{Question Answering over linked data}

\begin{table}
\centering
\caption{Motivating Example for KB QA}
\begin{tabular}{| p{1.5cm} | p{6cm} |} \hline
Question & Who is the woman that John Edwards had an affair with?\\
\hline
Provided answer & ``Writer'', ``Politician'', ``Lawyer'', ``Attorneys in the United States''\\
\hline
Correct answer & Rielle Hunter\\
\hline
QnA pair from Yahoo! Answers & Who was it that John Edwards had an affair with? Today, John Edwards admitted to having an affair with filmmaker Rielle Hunter.\\
\hline
Phrase from Wikipedia & John Edwards had engaged in an affair with Rielle Hunter...\\
\hline
\end{tabular}
\label{table:kbqa_example}
\end{table}


Systems for question answering over linked data need to have a big lexicon to be able to connect unstructured text with the corresponding parts of a structured query and they benefit from more training data.
I propose to use large collections of QnA pairs available on various CQA websites both for training and as additional signal for best answer selection.
For example, Table \ref{table:kbqa_example} shows an example of question from popular WebQuestions dataset \cite{BerantCFL13:sempre}, that is answered incorrectly by a state-of-the-art system.
A similar training example is missing in the dataset, however one (in this case even about the same entities) can be found on Yahoo! Answers.
For training, existing entity linking approaches can be used to find mentions of KB entities.
For QnA pairs with at least one entity in the question and answer it is possible to generate candidate structured queries given the question and treat the ones that produce the entities mentioned in the answer as correct ones (after some noise filtering).

However, the amount of labelled or weakly labelled information available for training is still very limited compared to the the amount of data available on the web.
Training also ``compresses'' the information present in the training set and some rare signals might get lost (e.g. due to feature selection).
For example, in some cases even if a question similar to the given test question was present during training, the model might not include enough information to answer it correctly.
This information can be used during testing as additional features.
The proposal is to extend the knowledge base representation with available textual information (Figure \ref{fig:kb_text}) by retrieving a set of existing QnA pairs, sentences or phrases mentionining question and candidate answers, as well as patterns used to expressed predicates from the constructed candidate KB query.
For example, in Table \ref{table:kbqa_example} we can find a sentence from Wikipedia, that may be used to support the candidate generating ``Rielle Hunter'' as the answer.

\begin{figure}
\centering
\includegraphics[width=0.5\textwidth]{img/kb_text}
\caption{Text annotation of KB}
\label{fig:kb_text}
\end{figure}


Finally, to solve the problem of compositionality of queries following the idea proposed in \cite{ReddyLS14} learn lexical features from raw sentences and their distantly supervised alignments to a KB, but avoid expensive and innaccurate semantic parsing step and learn direct associations between surface features and KB elements.
\textbf{Actually I don't have a very good idea how to do this, probably need to remove this paragraph}.