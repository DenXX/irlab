
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