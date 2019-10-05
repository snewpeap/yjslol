import requests
from bs4 import BeautifulSoup
import re
import pymongo.collection as mongoCollection
from pymongo import MongoClient
import time

client = MongoClient('localhost', 27017)
db = client.lol
games_collection = mongoCollection.Collection(db, 'games')
crawl_history = mongoCollection.Collection(db, 'crawl_history')

updateTime = int(time.time())
counter = 0

header = {
    "Host": "www.op.gg",
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:69.0) Gecko/20100101 Firefox/69.0",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "Accept-Language": "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2",
    "Accept-Encoding": "gzip, deflate, br",
    'Connection': 'keep-alive'
}


def highest_spider():
    global counter
    ladder_html = requests.get(
        url="https://www.op.gg/ranking/ladder/", headers=header, verify=False)
    soup = BeautifulSoup(ladder_html.text, 'lxml')
    highest_list = soup.find_all('li', id=re.compile('summoner-\\d+'))
    for summoner in highest_list:
        sid = str(summoner['id']).split('-')[1]
        name = str(summoner.find('a', class_='ranking-highest__name').string)
        rank = str(summoner.contents[0].string)
        tier = 'Challenger'
        print(rank+" SummonerID: "+sid+", name: "+name+", tier: "+tier)
        # ret_list.append({
        #     'sid': sid,
        #     'sname': name,
        #     'rank': rank,
        #     'tier': tier
        # })
        s = games_collection.find_one(
                    {'sid': sid}
                )

        reach_end = False
        if s != None and dict(s).get('reach_end') != None:
            reach_end = dict(s).get('reach_end')

        games_collection.update_one(
            {'sid': sid},
            {'$set': {
                'sid': sid,
                'sname': name,
                'rank': int(rank.strip()),
                'tier': tier,
                'updateAt': updateTime,
                'latest_game_time': 0,
                'reach_end': reach_end
            }},
            upsert=True
        )
        counter += 1


def spider():
    page = 1
    noMatch = True
    global counter

    while True:
        ladder_html = requests.get(
            "https://www.op.gg/ranking/ladder/page="+str(page), headers=header, verify=False)
        soup = BeautifulSoup(ladder_html.text, 'lxml')
        normal_list = soup.find_all(
            'tr', id=re.compile('summoner-\\d+'))
        for summoner in normal_list:
            tier = str(summoner.contents[2].string).strip()
            if tier == 'Challenger' or tier == 'Grandmaster' or tier == 'Master':
                noMatch = False
                sid = str(summoner['id']).split('-')[1]
                name = str(summoner.contents[1].a.span.string)
                rank = str(summoner.contents[0].string)
                print(rank+" 召唤师ID: "+sid +
                      ", 昵称: "+name+", 段位: "+tier)
                # ret_list.append({
                #     'sid': sid,
                #     'sname': name,
                #     'tier': tier
                # })
                s = games_collection.find_one(
                    {'sid': sid}
                )

                reach_end = False
                if s != None and dict(s).get('reach_end') != None:
                    reach_end = dict(s).get('reach_end')

                games_collection.update_one(
                    {'sid': sid},
                    {'$set': {
                        'sid': sid,
                        'sname': name,
                        'rank': int(rank.strip()),
                        'tier': tier,
                        'updateAt': updateTime,
                        'latest_game_time': 0,
                        'reach_end': reach_end
                    }},
                    upsert=True
                )
                counter += 1
        if noMatch:
            return
        else:
            noMatch = True
            page += 1


if __name__ == '__main__':
    crawl_history.update_one(
        {'spider': 'summoner'},
        {'$set': {'latest': updateTime}},
        upsert=True
    )
    highest_spider()
    spider()
    print('Done, totally ' + str(counter) + ' summoners.')
