I propose to combine available structured, semi-structured and unstructured data sources and do joint inference to improve question answering over both natural language text collections and knowledge bases.

\subsection{Text-based question answering}
\subsubsection{Using Knowledge Base for text-based QA}

For question answering over natural language document collections we propose to extend the text representation with annotations about mentioned entities and their relations from structured knowledge bases such as Freebase.
For example, below is one of the questions from TREC QA 2007:\\
\textit{``What republican senators supported the nomination of Harriet Miers to the Supreme Court?''}\\
A candidate answer sentence \textit{``Minority Leader Harry Reid had already offered his open support for Miers.''} mentions ``Harry Reid'' and clearly says about his support of the nomination.
However, ``Harry Reid'' happens to be a democrat and is not a part of the correct answer to the question.
To make the correct decisions I propose to do entity linking to connect a text mention to the corresponding KB entity and analyze the neighbourhood of the entity in the knowledge graph to discover a triple [Harry Reid, politician party, Democratic Party], which is incompatible with the phrase ``republican senator'' in the question.
Figure \ref{fig:text_kb} shows a joint text-KB representation of the question and candidate answer.

For other candidates using knowledge base as additional datasource may reveal specific connections between entities in the question and in the answer candidates.
For example, for another TREC QA 2007 question \textit{``For which newspaper does Krugman write?''} and an answer \textit{New York Times} a relation between [Paul Krugman, columns publisher, New York Times] gives an evidence in support of the candidate.

To do this kind of inference we propose to \textbf{PROVIDE DETAILS ON THE PROPOSED APPROACH: distant supervision patterns, direct connection to question phrases, etc.}, e.g. .

\begin{figure}
\centering
\includegraphics[width=0.5\textwidth]{img/text_kb}
\caption{KB annotation of text}
\label{fig:text_kb}
\end{figure}


\subsubsection{Using CQA data for text-based QA}
For non-factoid questions it is important to understand the language used in answers to express certain types of information.
Question answering systems benefit from more training data and I propose to use QnA pairs available on CQA websites as a training data for a model, which will learn associations between question intent and words and phrases used in the answer text.


\subsection{Question Answering over linked data}

Schema-based knowledge bases, such as Freebase, encode a lot of information into its graph structure and allows effective querying using special languages, e.g. SPARQL.
However, to answer natural language questions over linked data one needs to have a way translate unstructured text queries into such structured query languages.
Table \ref{table:kbqa_example} shows an example of question answered incorrectly by a state-of-the-art system.

\begin{table}
\centering
\caption{Motivating Example for KB QA}
\begin{tabular}{|p{8cm}|} \hline
Question: who is the woman that john edwards had an affair with?\\
\hline
Provided answer: "Writer", "Politician", "Lawyer", "Attorneys in the United States"\\
\hline
Correct answer: Rielle Hunter\\
\hline
Phrase from Wikipedia: \textbf{John Edwards} had engaged in an affair with \textbf{Rielle Hunter}...\\
\hline
\end{tabular}
\label{table:kbqa_example}
\end{table}

To address this problem I propose:
\begin{itemize}
\item following the idea proposed in \cite{ReddyLS14} learn lexical features from raw sentences and their distantly supervised alignments to a KB, but avoid expensive and innaccurate semantic parsing step and learn direct associations between surface features and KB elements
\item to bridge the gap between question and answer language models combine available training data with a large corpus of QnA pairs from CQA websites
\item use text passages mentioning KB entities and predicates as additional signals during candidate answer ranking
\end{itemize}

%Combining document and KB-based question answering:
%\begin{itemize}
%\item use more lexical information for KB-based question answering
%	\begin{itemize}
%	\item use document collection and/or web and generate more features for each structured query
%	\item borrow some ideas from relation extraction and distant supervision for KB-based QA
%	\end{itemize}
%\item use available KB information better for collection based question answering
%	\begin{itemize}
%	\item Universal schema and PRA combine knowledge graph links and text, we need to something similar to extent the document representation. This can help both factoid and non-factoid question answering.
%	\item previous research have found description in Freebase to be useful for question answering. We can try to add answer validation stage and query the web with the answer and read what is it about, which should validate it as the answer candidate. Probably this needs to be done in combination with the question, as for example the data isn't very productive
%	\end{itemize}
%\end{itemize}