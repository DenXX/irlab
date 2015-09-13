\subsection{Factoid question answering}
% I want to say, that systems either use text to get candidate answers, or they use knowledge bases. Hybrid systems usually have independent pipelines to get all candidates and then merge.
Most of the existing systems for factoid question answering are based either on unstructured natural language text collections or structured knowledge bases, or they take a hybrid approach and merge answer candidates obtained from multiple sources.

I propose to use all available data sources jointly....
CQA and the web and KB. Combine them!!!

\subsubsection{Text-based question answering}


\begin{table}
\centering
\caption{Motivating Example for Text QA}
\begin{tabular}{|p{8cm}|} \hline
What republican senators supported the nomination of Harriet Miers to Supreme Court?\\
\hline
/government/politician/party - Republican Party\\
\hline
There is no type for republican, but there is an edge in the graph\\
\hline
\end{tabular}
\label{table:textqa_example}
\end{table}


\subsubsection{Question Answering over linked data}
Existing approaches to question answering over linked data made a lot of progress, but the performance is still far from optimal.

Table \ref{table:kbqa_example} shows an example, not answered correctly by a state-of-the-art system.

\begin{table}
\centering
\caption{Motivating Example for KB QA}
\begin{tabular}{|p{8cm}|} \hline
Q: who is the woman that john edwards had an affair with? A: Rielle Hunter\\
\hline
"Writer","Politician","Lawyer","Attorneys in the United States"\\
\hline
\textbf{John Edwards} had engaged in an affair with \textbf{Rielle Hunter}...\\
\hline
\end{tabular}
\label{table:kbqa_example}
\end{table}

Artifacts of WebQuestions:
\begin{itemize}
\item Questions are very similar to one another, which is probably an artifact of data collection using Google Suggest (NEED TO SUPPORT THIS CLAIM WITH SOME STATS)
\item Answers were obtained from the Freebase page for the entity, which only includes neibours and nodes connected through CVT, which makes it possible to answer questions with very limited set of SPARQL query templates
\end{itemize}
We propose to develop a new dataset of questions that can be answered with Freebase based on QnA pairs posted to the CQA websites, e.g. Yahoo! Answers.
Extract questions that mention at least one entity in the question, at least one entity in the answer, filter (BY WHAT) and validate.


!!! The last idea makes a connection to LiveQA...


.....
A typical question answering system (Figure \ref{fig:qa_architecture}) retrieves a set of documents that might contain the answer and then extract and rank phrases as candidate answers.
The selection and ranking of candidates is usually guided by the expected type of the answer, answer paragraph language, popularity of the answer in the retrieved set of documents, etc.
Very few information is available on the answer itself.
Previous research found it useful to explore more information about the candidate answer, e.g. answer entity Wikipedia page (CITE SOMETHING), SOMETHING ELSE.

Recently \cite{Sun:2015:ODQ:2736277.2741651} proposed to use the large open-domain knowledge base (Freebase) to extend the expected answer types system to large fine-type system used in the knowledge base and use the textual description of the entity and its matches against the query text to rank the candidates.
However, in this work the graph nature of Freebase wasn't taken into account, i.e. relationships between entities mentioned in the question and candidate answers are not explored.
We believe that this information will improve the performance of QA system and propose to explore this direction.
... For each predicate we might also have a set of phrases used to express relationship in natural text (distant supervision for relation extraction) and it can also be used...



Community Question Answering websites (CQA), such as Yahoo! Answers\footnote{http://answers.yahoo.com/}, Quora\footnote{http://www.quory.com/}, Stack Exchange\footnote{http://stackexchange.com/}, Answers.com, etc, contain millions of question and answer (QnA) pairs from real users.
These QnA pairs were shown to be useful to answer future users questions.
...

\begin{figure*}
\centering
\epsfig{file=qa_architecture}
\caption{Architecture of typical QA system}
\label{fig:qa_architecture}
\end{figure*}

CQA, and text for KB QA!!!
KB for other, including answers using CQA.


Combining document and KB-based question answering:
\begin{itemize}
\item use more lexical information for KB-based question answering
	\begin{itemize}
	\item use document collection and/or web and generate more features for each structured query
	\item borrow some ideas from relation extraction and distant supervision for KB-based QA
	\end{itemize}
\item use available KB information better for collection based question answering
	\begin{itemize}
	\item Universal schema and PRA combine knowledge graph links and text, we need to something similar to extent the document representation. This can help both factoid and non-factoid question answering.
	\item previous research have found description in Freebase to be useful for question answering. We can try to add answer validation stage and query the web with the answer and read what is it about, which should validate it as the answer candidate. Probably this needs to be done in combination with the question, as for example the data isn't very productive
	\end{itemize}
\end{itemize}