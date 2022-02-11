# Taichi Auto Upload Script
# Copyright (C) 2022 qwq233
# 
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
# 
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.

import json
import os
import sys

import requests
import requests.utils

api_address = "http://admin.taichi.cool"


def login():
    headers = {
        'User-Agent': "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                      "Chrome/86.0.4240.111 Safari/537.36",
        'Cookie': "",
        'Content-Type': 'application/json'
    }
    data = json.dumps(dict(username=username, password=password))
    response = requests.post(api_address + "/auth/login", headers=headers, data=data, verify=True)
    return response.cookies.get_dict()


def upload(file):
    headers = {
        'User-Agent': "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                      "Chrome/86.0.4240.111 Safari/537.36"
    }
    apkfile = {"file": open(str(file), "rb")}
    cookies = requests.utils.cookiejar_from_dict(cookie)
    response = requests.post(api_address + "/upload", headers=headers, files=apkfile, cookies=cookies)
    return response.json()


def add():
    headers = {
        'User-Agent': "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                      "Chrome/86.0.4240.111 Safari/537.36",
        'Content-Type': 'application/json'
    }
    data = json.dumps(dict(aid=upload_response['data']['id'], name="QNotified-CI",
                           desc=upload_response['data']['desc'], magisk=True, log="ci update"))
    cookies = requests.utils.cookiejar_from_dict(cookie)
    response = requests.post(url=api_address + "/module/add", headers=headers, data=data, cookies=cookies)
    return response


if __name__ == "__main__":
    if not os.path.exists('./taichi.json'):
        os.mknod('./taichi.json')
    token_dump = open('./taichi.json', 'r+')
    if os.path.getsize('./taichi.json') == 0:
        username = input("username: ")
        password = input("password: ")
        token_dump.write(json.dumps(dict(username=username, password=password)))
    else:
        user = json.loads(token_dump.read())
        username = user['username']
        password = user['password']
    if len(sys.argv) == 2:
        cookie = login()
        print("Get Cookie")
        upload_response = upload(sys.argv[1])
        if upload_response["code"] == 0:
            print("Upload successfully")
            add = add().json()
            if add['code'] == 0:
                print("Add successfully")
            else:
                print("Add failed")
                print(add)
                exit(2)
        else:
            print("Upload failed")
            print(upload_response)
            exit(2)
    else:
        print("Usage:python taichi.py [Target File]")