\subsection{Factoid QA}

TREC QA datasets served as a benchmark for various question answering systems.
Therefore, to evaluate the proposed approach for question answering over text enriched with the structured data I propose to test it on dataset derived from TREC QA and compare against existing strong baselines, including the most related approaches \cite{Fader:2014:OQA:2623330.2623677,Sun:2015:ODQ:2736277.2741651}.
The proposed system can use the web as the corpus and query it using Bing Search API\footnote{https://datamarket.azure.com/dataset/bing/searchweb}.
Freebase and Reverb extractions \cite{FaderSE11} are examples of schema-based and open knowledge bases that can be used for the experiments.
The metrics used for evaluation typically include accuracy and mean reciprocal rank (MRR).

On the other hand, most of the recent work on knowledge base question answering and semantic parsing have been evaluated on the WebQuestions dataset \cite{BerantCFL13:sempre}, which contains a collection of question text and correct answer entities.
The questions were collected using Google Suggest API and answers crowdsourced using Amazon Mechanical Turk\footnote{http://mturk.com/}
The proposed approach will be compared against the previous results\footnote{http://goo.gl/sePBja} on this dataset.
Again, web can be used as a text collection which can be queried using Bing Search API.
To allow entity-based search of text-documents we can use ClueWeb12 collection and the corresponding entity mentions annotations \cite{gabrilovich2013facc1}.

However, both of these benchmarks have certain limitations.
TREC QA dataset is relatively small, and WebQuestions dataset is biased towards questions that can be relatively easily answered from Freebase (because answers were labelled using Freebase entity profile pages).
In my dissertation I propose to build a new dataset for factoid question answering, which is going to be based on questions posted to a CQA website.
Using simple heuristics it's possible to pre-filter a set of questions that can be further post-filtered and labelled using crowdsourcing.
More specifically, I propose to use Yahoo! Answers WebScope dataset of question-answer pairs, filter them to remove non-factoid and opinion questions using some simple heuristics, such as: keep single sentence questions only, that start with the question word (except why), do not contain personal pronouns, comparative and superlative adjectives.
Preliminary analysis revealed, that after the pre-filtering of 4.4M questions from the WebScope dataset, we have 70K questions left, from which $\sim$30\% are factoid.
I will further filter this dataset using Mechanical Turk workers, who will also annotate the correct answers by selecting one or more entities, mentioned in the answer text, provided on Yahoo! Answers.
This dataset will represent a sample of real user information needs, will be more general than WebQuestions dataset and larger than TREC QA.
In addition, since KB entities will be used to annotate answers, there will be no need in designing answer checking regular expressions, etc.
I'm going to make this dataset public and compare existing text-based and knowledge base question answering system along with the system I develop in my thesis.

\subsection{Non-factoid QA}

To evaluate the performance of the developed non-factoid QA system I'm going to participate in TREC LiveQA 2016 shared task\footnote{https://sites.google.com/site/trecliveqa2016/call-for-participation}.
I participated in the task in 2015 \cite{savenkov2015liveqa}, and the second attempt will allow me not only compare the developed system against other teams, but also check the progress of my system against the last year baseline.

