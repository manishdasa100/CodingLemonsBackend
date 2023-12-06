import requests
import random

PROBLEM_COUNT = 15

add_prob_api_url = "http://localhost:3000/api/v1/addProblem"
remove_prob_api_url = "http://localhost:3000/api/v1/removeAllProblems"

alldifficulties = ["EASY", "MEDIUM", "HARD"]
alltopics = ["Arrays", "Graph", "Tree", "BFS", "DFS", "Dynamic Programming", "Binary Search", "Bit Manipulation", "Linked Lists", "Stack", "Queue", "Priority Queue"]

def get_random_topics():
    topic_count = random.randint(1, 3)
    topics = set()
    for i in range(topic_count):
        n = random.randint(0,len(alltopics)-1)
        topics.add(alltopics[n])
    return topics

def get_random_difficulty():
    n = random.randint(0, 2)
    return alldifficulties[n]

def createProblem(id, difficulty, topics, acceptance):
    problem = {
        "title": id,
        "description": "Description of the problem" + str(id),
        "testCases": [
            "test1",
            "test2",
            "test3",
            "test4"
        ],
        "testCaseOutputs": [
            "testoutput1",
            "testoutput2",
            "testoutput3",
            "testoutput4"
        ],
        "difficulty": difficulty,
        "driverCode": "driver code",
        "optimalSolution": "optimal solution",
        "topics": list(topics),
        "acceptance": acceptance
    }
    return problem

response = requests.delete(remove_prob_api_url)

for i in range(PROBLEM_COUNT):
    problem = createProblem(i+1, get_random_difficulty(), get_random_topics(), random.randint(0, 100)) 
    response = requests.post(add_prob_api_url, json=problem)
    response.json()
    print(response)