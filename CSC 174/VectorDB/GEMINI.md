# Vector Databases & RAG: Concepts and Nomenclature

## 1. Foundational Concepts: The "what" and "why"

### Internal Knowledge Base & Proprietary Data
Large Language Models (LLMs) are typically trained on public data. To answer questions about domain-specific, private, or local data—such as university catalog rights or specific sales data—an **Internal Knowledge Base** is required. This allows the LLM to utilize external information it was not originally trained on.

### Embeddings
An embedding is the representation of an object (such as text) as a vector of numbers in a coordinate system.
* **Vector:** A list of floating-point numbers (e.g., `[0.004, -0.024, ...]`).
* **Dimensions:** The size of the vector is determined by the model used. For example, the `mxbai-embed-large` model may be used, or `llama3.2`, where the embedding size is specific to the model (e.g., 768 or 1024).

### Vector Database (Vector Store)
A database designed to handle internal knowledge bases by storing and querying vectors. Unlike relational databases that store specific values like "Professor" for exact matching, vector databases facilitate semantic retrieval, such as finding positive reviews about cakes.

---

## 2. The RAG Pipeline (Retrieval Augmented Generation)

**RAG** extends an LLM's capabilities to answer questions based on internal knowledge bases without retraining the model.

### Phase 1: Ingestion (ETL)
1.  **Load:** Documents are loaded from sources such as PDFs, Text files, CSVs, JSON, or Web pages.
2.  **Transform (Splitting):** Large documents are split into smaller chunks (e.g., using `RecursiveCharacterTextSplitter`) to maintain context and fit within model limits.
3.  **Embed:** Text chunks are converted into vectors using embedding models.
4.  **Store:** The Vector, the original text (Payload/Content), and Metadata are stored in the database.

### Phase 2: Retrieval & Generation
1.  **Query Embedding:** The user's question is converted into a vector using the same embedding model.
2.  **Similarity Search:** The database finds stored vectors closest to the query vector.
3.  **Augmentation:** The retrieved content is combined with a system prompt (e.g., "Answer the user's question using only this information") and passed to the LLM for the final answer.

---

## 3. Distance Metrics (Mathematical Similarity)

The definition of "closeness" depends on the metric used.

| Metric | Symbol (PGVector) | Description | Usage Context |
| :--- | :--- | :--- | :--- |
| **Euclidean Distance** (L2) | `<->` | The straight-line distance (square root of the sum of squared differences). | Standard distance metric; used in Qdrant and PGVector. |
| **Cosine Distance** | `<=>` | Calculated as $1 - \text{Cosine Similarity}$. Range is 0 to 2, where 0 indicates identical documents. | Common for text similarity. |
| **Manhattan Distance** (L1) | `<+>` | The sum of absolute differences (Taxicab distance). | Sparse high-dimensional data. |
| **Dot Product** | N/A | The sum of products of corresponding entries. | Often used in creating collections. |

---

## 4. Vector Database Implementations

### Chroma
* **Deployment:** Can run in-memory or be persistent (saved to disk).
* **Integration:** Works natively or via LangChain.
* **Indexing:** Uses HNSW (Hierarchical Navigable Small World) for indexing, as seen in metadata configurations.

### Qdrant
* **Structure:**
    * **Collection:** The container for vectors (similar to a table).
    * **Point:** A record containing the Vector, Payload, and ID.
    * **Payload:** Metadata and original text associated with the vector.
* **Operations:** Supports `Scroll` to paginate results and `Upsert` (Update/Insert) for batching data.

### PGVector (PostgreSQL)
* **Type:** An extension for PostgreSQL that enables storing embeddings and performing vector similarity searches.
* **Hybrid Search:** Allows combining standard SQL queries (e.g., filtering by rating) with vector distance operations in a single query.
* **Storage:** Embeddings are stored as a `vector` column type within standard SQL tables.

---

## 5. Advanced Nomenclature & AI Ethics

### Filtering Strategies
* **Metadata Filtering:** Narrowing the search scope using attributes associated with the vector (e.g., filtering by "source" or "rating").
* **Relevance Scores:** Filtering results based on a similarity threshold (cutoff) to ensure only highly relevant documents are retrieved.

### Orchestration
* **LangChain:** A framework that provides a standard interface to integrate different LLMs, vector stores, and document loaders, simplifying the RAG process.
* **Ollama:** A framework for hosting LLMs and embedding models locally, ensuring data privacy and zero API costs.

### AI Ethics & Guardrails
* **Hallucination:** When an LLM response is inaccurate or irrelevant.
* **Guardrails:** Mechanisms to validate inputs and outputs.
    * **PII Guard:** Detects and prevents sensitive data leaks (e.g., PII).
    * **Hallucination Guard:** Checks if the answer is grounded in the source text.
    * **Topic/Tone:** Can filter toxic language or ensure outputs match valid choices.