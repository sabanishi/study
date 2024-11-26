from flask import Flask, request
import json
from litellm import completion
import os
import sys
from datetime import datetime

app = Flask(__name__)


@app.route('/generate', methods=['POST'])
def generate():

    input = request.get_data(as_text=True)
    data = json.loads(input)
    
    print("")
    print("------------------------------------------")
    print("")
    
    receive_request_time = datetime.now().strftime('%Y-%m-%d %H:%M:%S:%f')
    print("Input:")
    print(data)
    print("")

    system_data = data.get('system', 'Default system message')
    user_data = data.get('user', 'Default user message')

    response = completion(
        model="gpt-3.5-turbo",
        messages=[
            {"role": "user", "content": user_data},
            {"role": "user", "content": system_data}
        ]
    )

    # 結果から出力を切り出す
    result = response.choices[0].message.content
    print("Output:")
    print(result)
    
    print("Receive Request Time:\t" + receive_request_time)
    print("Return Request Time:\t" + datetime.now().strftime('%Y-%m-%d %H:%M:%S:%f'))
    
    return result


if __name__ == "__main__":
    args = sys.argv
    api_key = args[1]
    os.environ["OPENAI_API_KEY"] = api_key
    app.run(port=5000)
