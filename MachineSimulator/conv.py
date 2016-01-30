lines = open('results.txt','r').readlines()
lines[:] = [line.strip() for line in lines if line.strip!='']

out = open('output.csv', 'a')
noPM = 'x'
for line in lines:
	if '---' in line:
		out.write(','+noPM+'\n')
	elif 'no PM' in line:
		noPM = line.split(':')[1].strip()
	elif ':' in line:
		out.write(','+line.split(':')[1].strip())

out.close()


