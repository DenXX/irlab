In this work I propose to enrich the input data representation for QA systems by combining available unstructured, semi-structured and unstructured data sources for joint reasoning, which can improve the performance of question answering over both text collections and knowledge bases.

\subsection{Unstructured data QA}

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
\item use existing approaches for document retrieval (e.g. web search usign question as a query as in \cite{tsai2015web}) and extracting answer candidates.
\item use existing entity linking approaches to find KB entities mentioned in a question and corresponding answer candidates.
\item for each mentioned entities extract a subgraph containing its neighbourhood up to certain number of edges away and paths to other mentioned entities.
\item follow maching learning approach for candidate answer ranking and extend the feature representaion with features derived from the subgraph analysis. Example of features are:
	\begin{itemize}
	\item features describing discovered connections between entities mentioned in a question and a candidate answer, such as indicators of the relations, combination of relations with words and n-grams from the questions, similarity between the relations and the question text (using tf-idf or embeddings representation), etc. Textual representation of the predicates in structured knowledge bases can be obtained either from its description or using patterns learned from a large collection using distant supervision \cite{MintzBSJ09}.
	\item features describing the entities mentioned in the answer, i.e. similarities between entity properties and question words, n-grams and phrases, etc.
	\end{itemize}
\end{itemize}

For training text-based QA model I propose to use available QnA pairs from community question answering websites, which represent real user tasks and after certain filtering can be a good fit for learning both factoid and non-factoid question answering systems.
The data can help learn more associations between the language used in questions and their corresponding answers, which can be encoded as conditional probabilities (e.g. $p(w_a|w_{q_1},...,w_{q_n}$, where $w_a$ is a word of the answer and $w_{q_i}$ is some subset of the question words), pointwise mutual information or by employing deep learning techniques \cite{WangN15}.

\subsection{Structured data QA}

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
Phrase from Wikipedia & John Edwards had engaged in an affair with Rielle Hunter...\\
\hline
QnA pair from Yahoo! Answers & Who was it that John Edwards had an affair with? Today, John Edwards admitted to having an affair with filmmaker Rielle Hunter.\\
\hline
\end{tabular}
\label{table:kbqa_example}
\end{table}

Systems for knowledge base question answering usually are based upon a lexicon, that helps them to map natural language question into the structured knowledge base query.
In some cases the lexicon is encoded into the features of the trained model, that ranks candidate structured queries \cite{yao-scratch-qa-naacl2015}.
I propose to extend candidate answers and the corresponding structured queries with the available unstructured data, e.g. include relation extraction patterns for used predicates, paragraphs of text mentioning involed entities, etc (see Figure \ref{fig:kb_text}).
For example, Table \ref{table:kbqa_example} shows an example of question from popular WebQuestions dataset \cite{BerantCFL13:sempre}, that is answered incorrectly by a state-of-the-art system.
A similar question is missing from the training set, however, an easy web search can retrieve a sentence that mention question and answer entities and give enough supporting evidence to answer this question correctly.

More speficically, I propose:
\begin{itemize}
\item Use one of the available state of the art systems, such as \cite{bastmore:cikm:2015:aquu}, as a baseline
\item Extend a set of features representing a candidate answer with features derived from retrieved unstructured data sources, for example:
	\begin{itemize}
	\item from large document collection (such as the web) retrieve a set of passages by querying a search system with question, question + answer, question and answer entities as queries.
	\item find mentions of answer entities in the passages and use some aggregated statistics as features for the corresponding candidates.
	\item for each candidate answer retrieve a set of patterns used to express the corresponding relation obtained using distant supervision from a large text collection and compute the similarities between these patterns and the question text to use as features.
	\end{itemize}
\end{itemize}

In addition, I propose to include QnA pairs from CQA websites as weakly labelled training data.
The example in \ref{table:kbqa_example} shows that systems trained on small labelled dataset might not generalize well to question not seen during training.
However, a similar quesition is actually present in Yahoo! Answers\footnote{http://answers.yahoo.com/}.
Therefore, using weakly labelled corpus derived from QnA pairs from a CQA websites can help a system to learn to answer questions of different type.
To do this, for QnA pairs with at least one entity in the question and answer it is possible to generate candidate structured queries given the question and treat the ones that produce the entities mentioned in the answer as correct ones (after some noise filtering).

\begin{figure}
\centering
\includegraphics[width=0.5\textwidth]{img/kb_text}
\caption{Text annotation of KB}
\label{fig:kb_text}
\end{figure}


% Finally, to solve the problem of compositionality of queries following the idea proposed in \cite{ReddyLS14} learn lexical features from raw sentences and their distantly supervised alignments to a KB, but avoid expensive and innaccurate semantic parsing step and learn direct associations between surface features and KB elements.
% \textbf{Actually I don't have a very good idea how to do this, probably need to remove this paragraph}.