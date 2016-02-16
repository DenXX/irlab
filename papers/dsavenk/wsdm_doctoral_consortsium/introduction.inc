The ability to answer user questions with precise and concise information is a hard problem with a long history of research.
The knowledge necessary to answer these questions is scattered across a variety of resources of various types.
Over the years of research in automatic question answering people have studied different unstructured (natural language text), semi-structured (tables, question and answer pairs) and strucutred (knowledge bases) resources for answer generation.
However, different types of data sources have their own advantages and limitations.
For example, a lot of world knowledge is encoded in raw text format, however, it's hard to reason beyond what is stated in a fragment of text.
On the contrary, modern large scale knowledge bases (KB), such as Freebase\footnote{http://www.freebase.com/}, dbPedia\footnote{http://wiki.dbpedia.org/}, YAGO\footnote{https://www.mpi-inf.mpg.de/departments/databases-and-information-systems/research/yago-naga/yago/}, etc., aggregate all the information about a particular entity and make it quite easy to query using special query languages, such as SPARQL.
Unfortunately, regular users would prefer to use natural text for their questions, which leads to a problem of mapping between text phrases and knowledge base entities and predicates.
In addition, knowledge bases are inherently incomplete\cite{Dong:2014:KVW:2623330.2623623} and miss a lot of entities, facts and predicates.
In my thesis, I propose to alleviate these disadvantages by combining different data sources together.
In particular, by finding mentions of KB entities in text documents we make it possible to get all the information about entities mentioned in a text fragment, as well as get all text fragments mentionining an entity or a pair of entities.
For example, below is one of the questions from TREC QA 2007 dataset: \textit{``What republican senators supported the nomination of Harriet Miers to the Supreme Court?''}.
A candidate answer sentence \textit{``Minority Leader Harry Reid had already offered his open support for Miers.''} mentions a senator ``Harry Reid'' and clearly says about his support of the nomination.
However, ``Harry Reid'' is not a correct answer to the question because he is a member of the Democratic party.
This information is not available in the answer candidate sentence, but it is present as one of the properties in Freebase: \texttt{[Harry Reid, political\_party, Democratic party]}.

Another big chunk of user questions cannot be answered with an entity, number or date.
These questions, usually referred to as non-factoid, include various types of information needs, including definitions, opinions, recommendations, procedures, etc.
A series of TREC LiveQA evaluation campaigns, started in 2015, targets non-factoid question answering and asks participants to develop a system to answer real user questions, posted to Yahoo! Answers\footnote{http://answers.yahoo.com/} CQA website, in real-time.
It was previously demonstrated \cite{shtok2012learning} that user needs repeat and a strategy of retrieving answer to similar past questions can be quite effective for answering new questions.
However, in many cases such questions do not exist or challenging to retrieve.
Alternative strategies include ranking text passages extracted from retrieved web documents, which is a challenging problem due to the lexical gap between question and answer texts.
Therefore, one would benefit from knowing what kind of questions does a paragraph of text answer.
This information can often be inferred from the structure of a web page, e.g. forums, FAQ pages, or estimated using title, subtitle and other page elements.
In my thesis I propose to make a better use of the structure of web documents where possible, e.g. by detecting the type of a web page and extracting its key components.
For other generic web documents and their passages I propose to predict how likely a passage can answer the given question using the advances in deep learning method for text processing, including sequence to sequence models \cite{sutskever2014sequence}.

