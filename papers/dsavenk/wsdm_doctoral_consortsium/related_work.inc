The field of question answering has a long history of research and dates back to 60s, when first systems attempted to provide a natural language interface to databases \cite{Simmons:1965:AEQ:363707.363732}.
In 70s and 80s the development of restricted domain knowledge bases set a task for question answering frameworks to assist users in solving their problem, which lead to the development of interactive question answering systems, e.g. \cite{shortliffe1976mycin}, \cite{woods1977lunar}.

The modern era of question answering research started with the exponential growth of information available in the World Wide Web.

Most of the work on question answering have focused on the so called \textit{factoid} questions.
The answers to these questions represent simple facts, that can be expressed as a word or a short phrase, which often represents a named entity (person, organization, location), date, etc.
The other types of questions are sometimes referred to in general as \textit{non-factoid} questions.

Since 1999 the annual Text Retrieval Conference (TREC)\footnote{http://trec.nist.gov} organized a number of open domain question answering shared tasks, e.g. see \cite{dang2007overview} for a review.
This year TREC piloted a LiveQA track\footnote{http://trec-liveqa.org/}, in which the participant systems had to answer various questions coming from real users of Yahoo! Answers\footnote{http://answers.yahoo.com/} in real time.

There are a number of surveys available on developments in the question answering area \cite{Kolomiyets:2011:SQA:2046840.2047162}.

Question Answering can be classified by the type of information used to answer questions into: text/web document collection, knowledge base or text document.
There are some hybrid systems, e.g. IBM WATSON, YODAQA, OPENQA, etc.

\subsection{Collection-based QA}
Review first collection algorithms.
Review AskMSR, AskMSR+ and redundancy-based techniques.

\subsection{KB-based QA}
Review QALD and work on webquestions dataset.

\subsection{Hybrid techniques}
Describe IBM WATSON, OPENQA, YODAQA.

OpenQA is an interesting technique as it tries to convert text into OpenIE set of triples and then query it.

Describe recent work that used entity linking to Freebase.

\subsection{Reading Comprehension}
McTest dataset. Maybe this part is not relevant.
However, merging reading comprehesion to regular QA seems natural and interesting.