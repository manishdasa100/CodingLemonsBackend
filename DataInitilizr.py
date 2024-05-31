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
        "title": "Title " + str(id),
        "description": "Description of the problem" + str(id),
        "constraints":[
            "constraint 1",
            "constraint 2",
            "constraint 3"
        ],
        "examples": [
            {
                "input":"input 1",
                "output":"output 1",
                "explanation":"Explanation 1"
            },
            {
                "input":"input 2",
                "output":"output 2",
                "explanation":"Explanation 2"
            }
        ],
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
        "driverCodes": {
            "JAVA":"import java.util.Scanner;class Main{public static void main(String[] args){Scanner sc=new Scanner(System.in);Solution solution=new Solution();while(sc.hasNext()){String str=sc.nextLine();System.out.println(solution.myFunction(str));}}}",
            "PYTHON":"Python driver code",
            "JAVASCRIPT":"Javascript driver code"
        },
        "cpuTimeLimit":1.0,
        "memoryLimit":128000.0,
        "stackLimit":32000,
        "topics": list(topics)
    }
    return problem

admin_auth_token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbjEiLCJpYXQiOjE3MTQyOTA3ODksImV4cCI6MTcxNDM3NzE4OX0.i5otRd3v1jw2PcA7qy3CVHvrvzl9whn2ooNfzP-Z5e4"

response = requests.delete(remove_prob_api_url, headers={"Authorization":"Bearer %s" %admin_auth_token})

for i in range(PROBLEM_COUNT):
    problem = createProblem(i+1, get_random_difficulty(), get_random_topics(), random.randint(0, 100)) 
    response = requests.post(add_prob_api_url, json=problem, headers={"Content-Type": "application/json","Authorization":"Bearer %s" %admin_auth_token})
    response.json()
    print(response.content)