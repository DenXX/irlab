To enrich the data representation for QA systems I propose to combine available structured, semi-structured and unstructured data sources and do joint inference to improve question answering over both natural language text collections and knowledge bases.

\subsection{Text-based question answering}
\subsubsection{Using Knowledge Base for text-based QA}

\begin{figure}
\centering
\includegraphics[width=0.5\textwidth]{img/text_kb}
\caption{KB annotation of text}
\label{fig:text_kb}
\end{figure}


For question answering over natural language document collections I propose to extend the text representation with annotations about mentioned entities and their relations from structured knowledge bases such as Freebase.
For example, below is one of the questions from TREC QA 2007:\\
\textit{``What republican senators supported the nomination of Harriet Miers to the Supreme Court?''}\\
A candidate answer sentence \textit{``Minority Leader Harry Reid had already offered his open support for Miers.''} mentions a senator ``Harry Reid'' and clearly says about his support of the nomination.
However, ``Harry Reid'' is not a correct answer to the question because he is a democrat.
This information is not available in the text fragment and a QA system needs to employ external information, such as knowledge present in a KB.
Similar to \cite{Sun:2015:ODQ:2736277.2741651} existing entity linking approaches can be used to find KB entities mentioned in the text and enrich the unstructured text with the corresponding subgraph from the knowledge base (see Figure \ref{fig:text_kb}).
For the provided example we can discover a triple [Harry Reid, politician party, Democratic Party], which give us an evidence that this candidate is not a match for a ``republican senator'' in the question.

In other cases using a KB as additional source of information may reveal specific connections between entities in the question and in the answer candidates.
For example, for another TREC QA 2007 question \textit{``For which newspaper does Krugman write?''} and an answer \textit{New York Times} a path between ``Paul Krugman'' and ``New York Times'' in the knowledge graph gives an evidence in support of the candidate.

To do this kind of inference I propose:
\begin{itemize}
\item use existing entity linking approaches to find KB entities mentioned in question and answer candidates
\item follow machine learning approach for candidate answer ranking and extend the feature representation with features derived from the KB subgraph analysis
	\begin{itemize}
	\item check if there are any connections between entities mentioned in the question and in a candidate answer and output features including connecting path, its combination with words and n-grams from the question, etc.
	\item output similarity features between entities connected to entities mentioned in a candidate answer and the text of the question. This similarity can be computed using either either textual representation such as tf-idf (obtained from the available textual description of predicates and entities or from surface patterns learned with distant supervision \cite{MintzBSJ09} over a large corpus of text) or using embeddings \cite{BordesCW14:emnlp}.
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