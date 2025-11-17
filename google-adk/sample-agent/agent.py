from google.adk.agents import Agent, SequentialAgent, ParallelAgent, LoopAgent
from google.adk.models.lite_llm import LiteLlm
from google.adk.runners import InMemoryRunner
from google.adk.tools import AgentTool, FunctionTool, google_search
from google.genai import types


def exit_loop():
    """Call this function ONLY when the critique is 'APPROVED', indicating the report is finished and no more changes are needed."""
    return {"status": "approved", "message": "Report approved. Exiting refinement loop."}

retry_config=types.HttpRetryOptions(
    attempts=5,  # Maximum retry attempts
    exp_base=7,  # Delay multiplier
    initial_delay=1, # Initial delay before first retry (in seconds)
    http_status_codes=[429, 500, 503, 504] # Retry on these HTTP errors
)
# Core Agents 

tech_researcher = Agent(
    name="TechResearcher",
    model=LiteLlm(model="ollama/qwen:0.5b"),
    instruction="""Research the latest AI/ML trends. Include 3 key developments,
the main companies involved, and the potential impact. Keep the report very concise (100 words).""",
    output_key="tech_research",  # The result will be stored with this key.
)

health_researcher = Agent(
    name="HealthResearcher",
    model=LiteLlm(model="ollama/qwen:0.5b"),
    instruction="""Research recent medical breakthroughs. Include 3 significant advances,
their practical applications, and estimated timelines. Keep the report concise (100 words).""",
    output_key="health_research",  # The result will be stored with this key.
)

finance_researcher = Agent(
    name="FinanceResearcher",
    model=LiteLlm(model="ollama/qwen:0.5b"),
    instruction="""Research current fintech trends. Include 3 key trends,
their market implications, and the future outlook. Keep the report concise (100 words).""",
    output_key="finance_research",
)

# Refinement Agents + Workflow

tech_critic_agent = Agent(
    name="TechCritic",
    model=LiteLlm(model="ollama/qwen:0.5b"),
    instruction="""You are a senior tech analyst. Review the research report below.
    
    Report: {tech_research}
    
    Check for clarity, depth, and concise reporting (must be ~100 words).
    - If the report is excellent, you MUST respond with the exact phrase: "APPROVED"
    - Otherwise, provide 2-3 specific, actionable suggestions for improvement.""",
    output_key="tech_critique", # Use a unique critique key
)

tech_refiner_agent = Agent(
    name="TechRefiner",
    model=LiteLlm(model="ollama/qwen:0.5b"),
    instruction="""You are a tech report refiner.
    
    Original Report: {tech_research}
    Critique: {tech_critique}
    
    - IF the critique is EXACTLY "APPROVED", you MUST call the `exit_loop` function and nothing else.
    - OTHERWISE, rewrite the report to fully incorporate the feedback from the critique.""",
    tools=[FunctionTool(exit_loop)],
    output_key="tech_research", # CRITICAL: Overwrites the original report
)


tech_refinement_loop = LoopAgent(
    name="TechRefinementLoop",
    sub_agents=[tech_critic_agent, tech_refiner_agent],
    max_iterations=2,
)

tech_workflow = SequentialAgent(
    name="TechWorkflow",
    sub_agents=[tech_researcher, tech_refinement_loop]
)




# HEALTH AGENT 
health_critic_agent = Agent(
    name="HealthCritic",
    model=LiteLlm(model="ollama/qwen:0.5b"),
    instruction="""You are a senior medical editor. Review the research report below.
    
    Report: {health_research}
    
    Check for accuracy, practical application, and concise reporting (must be ~100 words).
    - If the report is excellent, you MUST respond with the exact phrase: "APPROVED"
    - Otherwise, provide 2-3 specific, actionable suggestions for improvement.""",
    output_key="health_critique",
)

health_refiner_agent = Agent(
    name="HealthRefiner",
    model=LiteLlm(model="ollama/qwen:0.5b"),
    instruction="""You are a health report refiner.
    
    Original Report: {health_research}
    Critique: {health_critique}
    
    - IF the critique is EXACTLY "APPROVED", you MUST call the `exit_loop` function and nothing else.
    - OTHERWISE, rewrite the report to fully incorporate the feedback from the critique.""",
    tools=[FunctionTool(exit_loop)],
    output_key="health_research", # Overwrites the original report
)

health_refinement_loop = LoopAgent(
    name="HealthRefinementLoop",
    sub_agents=[health_critic_agent, health_refiner_agent],
    max_iterations=2,
)

health_workflow = SequentialAgent(
    name="HealthWorkflow",
    sub_agents=[health_researcher, health_refinement_loop]
)

# FINANCE AGENT

finance_critic_agent = Agent(
    name="FinanceCritic",
    model=LiteLlm(model="ollama/qwen:0.5b"),
    instruction="""You are a senior financial analyst. Review the research report below.
    
    Report: {finance_research}
    
    Check for market relevance, clear implications, and concise reporting (must be ~100 words).
    - If the report is excellent, you MUST respond with the exact phrase: "APPROVED"
    - Otherwise, provide 2-3 specific, actionable suggestions for improvement.""",
    output_key="finance_critique",
)

finance_refiner_agent = Agent(
    name="FinanceRefiner",
    model=LiteLlm(model="ollama/qwen:0.5b"),
    instruction="""You are a finance report refiner.
    
    Original Report: {finance_research}
    Critique: {finance_critique}
    
    - IF the critique is EXACTLY "APPROVED", you MUST call the `exit_loop` function and nothing else.
    - OTHERWISE, rewrite the report to fully incorporate the feedback from the critique.""",
    tools=[FunctionTool(exit_loop)],
    output_key="finance_research", # Overwrites the original report
)

finance_refinement_loop = LoopAgent(
    name="FinanceRefinementLoop",
    sub_agents=[finance_critic_agent, finance_refiner_agent],
    max_iterations=2,
)

finance_workflow = SequentialAgent(
    name="FinanceWorkflow",
    sub_agents=[finance_researcher, finance_refinement_loop]
)


# Define Parallel and Sequential Agents 
parallel_research_team = ParallelAgent(
    name="ParallelResearchTeam",
    sub_agents=[
        tech_workflow,
        health_workflow,
        finance_workflow
    ],
)

aggregator_agent = Agent(
    name="AggregatorAgent",
    model=LiteLlm(model="ollama/qwen:0.5b"),
    instruction="""Combine these three approved research findings into a single executive summary:

    **Technology Trends:**
    {tech_research}
    
    **Health Breakthroughs:**
    {health_research}
    
    **Finance Innovations:**
    {finance_research}
    
    Your summary should highlight common themes, surprising connections, and the most important key takeaways from all three reports. The final summary should be around 200 words.""",
    output_key="executive_summary",
)



root_agent = SequentialAgent(
    name="ResearchSystem_With_Evaluators",
    sub_agents=[parallel_research_team, aggregator_agent],
)