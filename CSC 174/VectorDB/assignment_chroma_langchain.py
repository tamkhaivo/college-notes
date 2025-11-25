import os
from langchain_community.document_loaders import PyPDFLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_ollama import OllamaEmbeddings, OllamaLLM
from langchain_community.vectorstores import Chroma
from langchain_classic.chains import RetrievalQA
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.runnables import RunnablePassthrough
from langchain_core.output_parsers import StrOutputParser
from langchain_core.documents import Document


def main():
    # --- Configuration ---
    pdf_path = "MS_CSC_Catalog.pdf"
    collection_name = "langchain_chroma_grad"
    embedding_model = "embeddinggemma" # Or "mxbai-embed-large"
    llm_model = "granite4" # Or "llama3"

    # Clean up existing database for a fresh start (optional, but good for assignments)
    # if os.path.exists("./chroma_db"):
    #     shutil.rmtree("./chroma_db")

    # 1. Create a vector memory store of Chroma. Create a collection named “langchain_chroma_grad”.
    print("--- Task 1: Initialize Embeddings and Vector Store Setup ---")
    embeddings = OllamaEmbeddings(model=embedding_model)
    
    # We will initialize the vector store after processing documents to add them in one go, 
    # or we can initialize it empty. Let's process documents first.

    # 2. Process “MS_CSC_Catalog.pdf”. Use Langchain’s PDF loader and Splitter.
    print(f"--- Task 2: Process {pdf_path} ---")
    if not os.path.exists(pdf_path):
        print(f"Error: {pdf_path} not found.")
        return

    loader = PyPDFLoader(pdf_path)
    pages = loader.load()
    print(f"Loaded {len(pages)} pages.")

    text_splitter = RecursiveCharacterTextSplitter(
        chunk_size=1000,
        chunk_overlap=100,
        separators=["\n\n", "\n", ". ", " "]
    )
    splits = text_splitter.split_documents(pages)
    print(f"Split into {len(splits)} chunks.")

    # 3. Add the processed pdf files to the vector store collection.
    print("--- Task 3: Add documents to Chroma Vector Store ---")
    # Generate explicit IDs
    ids = [str(i) for i in range(len(splits))]
    
    # Initialize Chroma and add documents    
    vectorstore = Chroma.from_documents(
        documents=splits,
        embedding=embeddings,
        collection_name=collection_name,
        ids=ids
        # persist_directory="./chroma_db" # Optional: Uncomment to persist
    )
    print(f"Added {len(splits)} documents to collection '{collection_name}'.")

    # 4. Use get() to retrieve all documents in the vector store.
    print("\n--- Task 4: Retrieve all documents ---")
    all_docs = vectorstore.get()
    print(f"Retrieved {len(all_docs['ids'])} documents.")
    print(all_docs) # Commented out to avoid cluttering output, but this is the retrieval.

    # 5. Use get() to retrieve page 0 information.
    print("\n--- Task 5: Retrieve page 0 information ---")
    page_0_docs = vectorstore.get(where={"page": 0})
    print(f"Found {len(page_0_docs['ids'])} chunks from page 0.")
    if page_0_docs['ids']:
        print(f"First chunk of page 0 content preview: {page_0_docs['documents'][0][:100]}...")

    # 6. Use get() to retrieve id 0.
    print("\n--- Task 6: Retrieve ID 0 ---")
    id_0_doc = vectorstore.get(ids=["0"])
    print(f"ID 0 Content: {id_0_doc['documents'][0][:100]}...")

    # 7. Update the content of id 0.
    print("\n--- Task 7: Update content of ID 0 ---")
    prefix_text = "This document is the Catalog of Master Program (MS) in Computer Science. "
    current_content = id_0_doc['documents'][0]
    new_content = prefix_text + current_content
    
    # Update using the vectorstore's update_document method
    current_metadata = id_0_doc['metadatas'][0]
    updated_doc = Document(page_content=new_content, metadata=current_metadata)
    
    vectorstore.update_document(document_id="0", document=updated_doc)
    print("Updated ID 0.")
    
    # Verify update
    updated_id_0 = vectorstore.get(ids=["0"])
    print(f"New ID 0 Content Start: {updated_id_0['documents'][0][:100]}...")

    # 8. Query with similarity_search_with_relevance_scores() with 4 results.
    print("\n--- Task 8: Similarity Search with Relevance Scores (k=4) ---")
    query = "what are the admission requirements?"
    results_with_scores = vectorstore.similarity_search_with_relevance_scores(query, k=4)
    
    print(f"Query: {query}")
    for doc, score in results_with_scores:
        print(f"[Score: {score:.4f}] {doc.page_content[:100]}...")

    # 9. Cutoff score and similarity_search_with_relevance_scores().
    print("\n--- Task 9: Similarity Search with Cutoff Score ---")
    cutoff = 0.25

    print(f"Applying cutoff score: {cutoff}")
    filtered_results = [
        (doc, score) for doc, score in results_with_scores 
        if score >= cutoff
    ]
    
    if not filtered_results:
        print("No results met the cutoff score.")
    else:
        for doc, score in filtered_results:
            print(f"[Score: {score:.4f}] {doc.page_content[:100]}...")

    # 10. Use Langchain ChatPromptTemplate to construct a chain to perform RAG process.
    print("\n--- Task 10: RAG with ChatPromptTemplate ---")
    llm = OllamaLLM(model=llm_model, temperature=0)
    
    template = """Answer the question based only on the following context:
    {context}
    
    Question: 
    {question}
    """
    prompt = ChatPromptTemplate.from_template(template)
    retriever = vectorstore.as_retriever(search_kwargs={"k": 4})
    
    def format_docs(docs):
        return "\n\n".join([d.page_content for d in docs])

    chain = (
        {"context": retriever | format_docs, "question": RunnablePassthrough()}
        | prompt
        | llm
        | StrOutputParser()
    )
    
    rag_response = chain.invoke(query)
    print(f"Question: {query}")
    print(f"Answer: {rag_response}")

    # 11. Use Langchain RetrievalQA to do RAG process.
    print("\n--- Task 11: RAG with RetrievalQA ---")
    qa_chain = RetrievalQA.from_chain_type(
        llm=llm,
        chain_type="stuff",
        retriever=retriever,
        return_source_documents=True
    )
    
    qa_response = qa_chain.invoke(query)
    print(f"Question: {query}")
    print(f"Answer: {qa_response['result']}")

    # 12. Run at least 2 other queries.
    print("\n--- Task 12: Run 2 other queries ---")
    queries = [
        "What is the minimum GPA requirement?",
        "How many units are required for the master's degree?",
        "What are the Software Engineering courses provided?"
    ]
    
    for q in queries:
        print(f"\nQuery: {q}")
        res = qa_chain.invoke(q)
        print(f"Answer: {res['result']}")

    # 13. Delete Id 0 and verify.
    print("\n--- Task 13: Delete ID 0 ---")
    vectorstore.delete(ids=["0"])
    
    # Verify
    deleted_check = vectorstore.get(ids=["0"])
    if not deleted_check['ids']:
        print("Successfully deleted ID 0 (not found in store).")
    else:
        print("Error: ID 0 still exists.")

if __name__ == "__main__":
    main()
