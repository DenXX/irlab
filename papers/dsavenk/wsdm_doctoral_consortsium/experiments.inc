\subsection{Unstructured QA}

TREC QA datasets served as a benchmark for various question answering systems.
Therefore, to evaluate the proposed approach for question answering over text enriched with the structured data I propose to test it on dataset derived from TREC QA and compare against existing strong baselines, including the most related approaches \cite{Sun:2015:ODQ:2736277.2741651,Fader:2014:OQA:2623330.2623677}.
The proposed system can use the web as the corpus and query it using Bing Search API\footnote{https://datamarket.azure.com/dataset/bing/searchweb}.
Freebase and Reverb extractions \cite{FaderSE11} are examples of schema-based and open knowledge bases that can be used for the experiments.
The metrics used for evaluation typically include accuracy and mean reciprocal rank (MRR).

For non-factoid question answering this year TREC pioneered a new question answering track - TREC LiveQA\footnote{http://trec-liveqa.org/}, which targets questions asked by real users of Yahoo! Answers.
This year the deadline for system submission was on August 31 and my system trained on CQA QnA pairs participated in the challenge.
The results will be available on the TREC Conference in November 2015.
Organizers plan to continue with another TREC LiveQA task next year and this is going to be a good estimatation of the effectiveness of the proposed techniques on hard real user questions.

\subsection{Structured QA}

Most of the recent work on knowledge base question answering and semantic parsing have been evaluated on the WebQuestions dataset \cite{BerantCFL13:sempre}, which contains a collection of question text and correct answer entities.
The questions were collected using Google Suggest API and answers crowdsourced using Amazon Mechanical Turk\footnote{http://mturk.com/}
Since the questions in the dataset come from Google search logs, it is a better approximation of real user needs and is cheaper to obtain than some previous benchmarks, e.g. Free917.
The proposed approach will be compared against the previous results\footnote{http://goo.gl/sePBja} on this dataset.
Again, web can be used as a text collection which can be queried using Bing Search API.
Relation extraction patterns can be mined using distant supervision from Clueweb using publicly available dataset of Freebase annotations \cite{gabrilovich2013facc1}.

However, WebQuestions dataset has certain limitations:
\begin{itemize}
\item limited variability of the questions and their lexicon. As an artifact of using Google Suggest API as a data source, many questions from the dataset use very similar lexicon and structure to ask for some specific information.
\item limited variability of correct logical forms. The correct answers to the questions were labeled using the entity profile pages on the freebase website, which contain only entities connected directly or through a mediator node. Therefore most of the state-of-the-art results on the dataset use a small number of predefined logical form patterns.
\end{itemize}

On the other hand CQA websites have a fraction of factoid questions with provided text answers.
Author's previous research \cite{SavenkovLDA15} demonstrated that QnA pairs from CQA websites can be used for relation extraction.
Here I propose to use a similar way to construct a new dataset for question answering over Freebase by selecting a subset of QnA pairs with at least one entity in question and answer and some reasonable filtering heuristics and manual validation using crowdsourcing (e.g. through Amazon Mechanical Turk).
Existing systems need to be retrained and tested on the new dataset to compare against the proposed model.

\subsection{Combined QA}
To compare the proposed data enrichment approaches against the other available hybrid question answering systems I propose to use a dataset derived from TREC, such as the one used to evaluate the YodaQA system\footnote{https://github.com/brmson/dataset-factoid-curated}.
The system combining unstructured and structured approaches to question answering can be run in parallel with results ranking using a machine learning model.