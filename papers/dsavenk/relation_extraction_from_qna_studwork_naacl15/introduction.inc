Recently all major search companies have adopted knowledge bases (KB), and as a result users now can get rich structured data as answers to some of their questions.
However, even the largest existing knowledge bases, such as Freebase \cite{bollacker_sigmod2008}, DPpedia \cite{auer2007dbpedia}, NELL \cite{Carlson10}, Google Knowledge Graph \etc, which store billions of facts about millions of entities, are far from being complete \cite{Dong:2014:KVW:2623330.2623623}.
A lot of information is hidden in unstructured data, such as natural language text, and extracting this information for knowledge base population (KBP) is an active area of research \cite{surdeanu2014overview}.

One particularly interesting source of unstructured text data is CQA websites (\eg Yahoo! Answers,\footnote{http://answers.yahoo.com/} Answers.com,\footnote{http://www.answers.com} \etc), which became very popular resources for question answering.
The information expressed there can be very useful, for example, to answer future questions \cite{Shtok:2012:LPA:2187836.2187939}, which makes it attractive for knowledge base population.
There are certain limitations in applying existing relation extraction algorithms to CQA data, \ie, they typically consider sentences independently and ignore the discourse of QnA pair text.
However, often it is impossible to understand the answer without knowing the question.
For example, in many cases users simply give the answer to the question without stating it in a narrative sentence (\eg ``\emph{What does "xoxo" stand for? Hugs and kisses.}``), in some other cases the answer contains a statement, but some important information is omitted (\eg ``\emph{What's the capital city of Bolivia? Sucre is the legal capital, though the government sits in La Paz}``).
% or ``\emph{Who does the voice for Henry the Octopus on The Wiggles? Jeff Fatt aka Sleepy Jeff}''

% or ``\emph{Which soccer team has won the most European club championships? Real Madrid has won 9 titles, followed by 6 for AC Milan and 5 for Liverpool}'').

In this work we propose a novel model for relation extraction from CQA data, that uses discourse of a QnA pair to extract facts between entities mentioned in question and entities mentioned in answer sentences.
The conducted experiments confirm that many of such facts cannot be extracted by existing sentence-based techniques and thus it is beneficial to combine their outputs with the output of our model. 
% Although it is true, that some of the extracted facts can also be found in some other text documents, CQA websites provide additional opportunities, \eg, crowdsourcing knowledge by posting questions to the communities.

% One made an experiment to check how many relation instances that we can find in CQA documents in ClueWeb12\footnote{http://www.lemurproject.org/clueweb12/} can also be located in some other documents in the collection.
% It turned out that 8 \% of triples occur uniquely in CQA documents.