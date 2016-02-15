In this section I describe the proposed research for both factoid and non-factoid question answering in more details.

\subsection{Factoid Question Answering}

In my thesis I propose to annotate text document collection with links to mentioned knowledge base entities.
Such semantic annotations open up many opportunities for QA reasoning, because it allows one to go from the information stored in text to structured data and vice versa.

More specifically, I propose the following factoid QA system architecture:
\begin{itemize}
\item \textbf{Pre-processing}: identify mentions of KB entities in text document collection and index the documents text and mentions in separate fields
\item \textbf{Topical entity identification}: search the text collection using question (or reformulated question \cite{AgichteinLG01}) as a query and use an approach similar to \cite{cornolti2014smaph} to detect question topical entities
\item \textbf{Candidate generation from text}: extract candidate answer (or intermediate answer) entities with evidence from the retrieved text documents using existing techniques, e.g. \cite{tsai2015web}.
\item \textbf{Candidate generation from KB}: explore the KB neighborhood of question topical entities and entities extracted from text documents on the previous step
\item \textbf{Candidate generation from KB \& Text}: use entity and text index to find entities mentioned near question topical entity and question terms in the document collection
\item \textbf{KB evidence extraction}: match neighbourhood of answer entities (entity type and other entities) against the question to get additional evidence
\item \textbf{Text evidence extraction}: estimate the similarity between the collection text fragments mentionining question and answer entities and the question text
\item \textbf{Rank candidate}: rank candidate answers using evidence extracted from the KB as well as from text
\end{itemize}

For example, for the question mentioned in the introduction \textit{``What republican senators supported the nomination of Harriet Miers to the Supreme Court?''} and a candidate answer sentence \textit{``Minority Leader Harry Reid had already offered his open support for Miers.''}, such joint text-KB representation can look like Figure \ref{fig:kb2text}.
A QA system can discover that ``Harry Reid'' political affiliation is with the Democratic Party, and he cannot be referred to as ``republican senator''.
In other cases using a KB as an additional source of information may reveal specific connections between entities in the question and in the answer candidates.
For example, for another TREC QA 2007 question \textit{``For which newspaper does Krugman write?''} and retrieved candidate answer \textit{New York Times} a path between ``Paul Krugman'' and ``New York Times'' in the knowledge graph gives an evidence in support of the candidate.

\begin{figure*}
\centering
 \begin{subfigure}[t]{0.47\textwidth}
 \includegraphics[width=\textwidth]{img/text_kb}
 \caption{Annotation of natural language text with mentioned entities and their subgraphs in a knowledge base}
 \label{fig:kb2text}
 \end{subfigure}
 \begin{subfigure}[t]{0.47\textwidth}
 \centering
 \includegraphics[width=\textwidth]{img/kb_text}
 \caption{Annotation of KB graph nodes and edges with unstructured text data}
  \label{fig:text2kb}
 \end{subfigure}
\label{fig:text_kb}
\caption{Unstructured text and structured Knowledge Base connected via entity links for question answering}
\end{figure*}

Knowledge base question answering (KBQA) produce answers by constructing a structured query, that retrieves answer entities from the KB.
The main challenge in KBQA is mapping between natural language phrases in the question and knowledge base entities and predicates.
Such systems typically rely on the lexicon learned from the training data \cite{bastmore:cikm:2015:aquu,BerantCFL13:sempre,BerantL14:parasempre,yih:ACL:2015:STAGG,yao-scratch-qa-naacl2015}.
Such lexicons are often limited and needs to be retrained to include additional data.
The proposed approach allows a system to dig into the text resources that mention question and candidate answer pairs and use this information for scoring.
Figure \ref{fig:text2kb} shows a sample of data available for KBQA system to answer the \textit{`` Who is the woman that John Edwards had an affair with?''} question from a popular WebQuestions dataset \cite{BerantCFL13:sempre}.


\subsection{Non-factoid Question Answering}

Non-factoid questions are typically answered with a relatively long paragraph of text\footnote{TREC LiveQA'15 challenge limits the answer to 1000 characters}.
This fact and the nature of questions limits the utility of structured KB resources.
One of the main challenges for non-factoid question answering is matching between the question needs and the information expressed in text fragment.
Analysis of TREC LiveQA 2015 participants \cite{savenkov2015liveqa} revealed that the quality of answers extracted from previously posted similar questions is typically higher than from regular web passages.
Therefore, non-factoid QA system would benefit from the information on which questions does a paragraph of text answer.
This information can often be extracted from the structure of a web document, e.g. forum threads, FAQ pages or various CQA websites.
Alternatively, we can train a model to predict whether a paragraph answers a given question using titles, subtitles and surrounding text of a web page.

My proposal for non-factoid question answering can be summarized as follows:
\begin{itemize}
\item \textbf{CQA candidate generation}: retrieve a set of question-answer pairs by search a CQA archive\footnote{https://answers.yahoo.com/}
\item \textbf{Web document retrieval}: retrieve a set of documents by querying web search with the question (and queries generated from it)
\item \textbf{Web candidate answer generation}: classify web page into one of the following types: article, forum thread, FAQ page, CQA page, other. Extract key elements using type-specific extractors (QnA pairs FAQ and CQA pages, forum question and posts and article passages with the corresponding titles, subtitles and surrounding text).
\item \textbf{Ranking}: Rank the generated candidate answers
\end{itemize}

==============


More specifically, to do this kind of inference I propose:
\begin{itemize}
\item use existing approaches for document retrieval (e.g. web search using question as a query \cite{tsai2015web}) and candidate answer extraction.
\item perform entity linking to mentions of KB entities in questions and corresponding candidate answers.
\item for each mentioned entity extract a subgraph containing its neighborhood up to certain number of edges away and paths to other mentioned entities.
\item follow machine learning approach for candidate answer ranking and extend the feature representation with features derived from subgraph analysis. Examples of features are:
	\begin{itemize}
	\item features describing discovered connections between entities mentioned in a question and a candidate answer, such as indicators of the relations, combination of relations with words and n-grams from the questions, similarity between the relations and the question text (using tf-idf or embeddings representation), etc. Textual representations of the predicates in structured knowledge bases can be obtained either from its description or using patterns learned from a large collection using distant supervision \cite{MintzBSJ09}.
	\item features describing the entities mentioned in the answer, i.e. similarities between entity properties and question words, n-grams and phrases, etc.
	\end{itemize}
\end{itemize}
