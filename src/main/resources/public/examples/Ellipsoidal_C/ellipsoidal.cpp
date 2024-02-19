#include <iostream>
#include <cmath>


int main(int argc, char* argv[])
{
	int n = argc - 1;
	
	float s = 0.0;
	for(int i = 0; i < n; ++i) {
		float x = atof(argv[i+1]);
		s += pow(10.0,6.0*i/(n-1))*x*x/100000;
	}
	std::cout << "ellipsodial " << s << std::endl;
	
	return 0;
}